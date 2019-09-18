## Money Transfer API with Event Sourcing and CQRS

This project facilitates the transfer of money between two demo accounts.

Table of Contents
===

   * [Introduction and Quick Overview](#introduction)
   * [Why Event Sourcing and CQRS?](#why-event-sourcing-and-cqrs)
   * [Project Structure](#project-structure)
   * [Sample transfer scenarios with redudancy built in](#scenarios)
   * [Running the project](#running)
   * [Usage](#usage)
   * [Performance Testing](#performance-testing)
   * [Final Thoughts](#final-thoughts)
    
    

Introduction
===

The purpose of this project is to build a high quality money transfer api. This is a demo project this can still be used in a production environment. The following technologies are used:

- `H2`: An in memory database
- `Akka and Akka Persistence`: For event sourcing, message delivery and concurrency 
- `Akka HTTP`: Web Server
- `Zerocode`: A load testing tool
- `Maven`: Build tool
- `Java 8`: Used for everything :wink:

Since the primary aim of this system is to demo a money transfer api, the following assumptions/decisions were made

+ A default currency of euros is used, so no currency conversion or any other currency feature were built into the system.
+ No provision for creating bank accounts, so five bank accounts (with ids `1, 2, 3, 4, 5`) are created by default each with a balance of *10,000*. In this universe, that's your opening balance with Revolut. :smile:
+ Another bank account with an id of `10` was created as well, to demonstrate rollback and unavailability.

Why Event Sourcing and CQRS
===

The concept of transferring money is centered around immutable facts `I transferred xy` etc, and all these facts needs to be saved in an append only store for the purpose of the audit trail. Audit trail is a first class citizen of event sourcing, and not an after thought by having a history table.

CQRS provides us with the ability to scale independent part of our systems, and provide better capability to display our data since the event sourcing part only uses an append log.

Project Structure
===

The project makes use of just one maven project with a root package `io.kiamesdavies.revolut` and the following sub packages:

- `account`: Contains the bank and account actors 
- `commons`: Contains utility classes
- `controllers`: Contains the directives to expose the application for HTTP access
- `exceptions`: Custom exceptions
- `models`: Contains events, commands and domain models used in the application
- `services`: Contains the API and a default implementation for the money Transfer.

For a quick glance, there are four classes *(DefaultAccount, TransferHandler, Bank and BankAccount)* responsible for the majority of the functionalities in the system. 


Scenarios
===

This project is based on the actor and let it crash model. Its almost impossible to build computer systems with the expectation or goal of it not crashing, there are several moving parts, rather build in  fault handling and redundancy, and according to Murphy's law *"whatever can go wrong will go wrong"*.

This is what **Akka** brings to the JVM, a simple and clear way to describe who is responsible for fault handling (through supervision and monitoring actors)  and those responsible for the actual business logic. 

Lets first go through an ideal scenario of a user transferring €100 from  account A to another account B, then we will explore other scenarios of system crash at different sections and their fault handling.

### Ideal Scenario
<img  alt="para yaml" width="837" src="https://user-images.githubusercontent.com/3046068/65124468-c14bf280-d9ec-11e9-9631-59e1f8724e9e.png">
<br/>
<br/>
The image above pretty explains it all.

**Note:** the server respond to the user that the transfer is complete while deposit is on going, this is to reduce waiting time by the user, and for situations that requires accessing external resources like transferring to another bank, this is definietely the way to go. 

### Possible Real life Scenario

<img  alt="rollback" width="837" src="https://user-images.githubusercontent.com/3046068/65185432-71f2da00-da5f-11e9-8aab-c5597323880d.png">  
<br/>

Crash at the sender's account


<img  alt="rollback" width="837" src="https://user-images.githubusercontent.com/3046068/65185465-846d1380-da5f-11e9-9481-3c8a5ab738e7.png">  
<br/>

Crash at the receiver's account


As shown in the images above, there are two points this crash matters for every bank account
+ Before persisting the withdrawal or deposit event
+ After persisting the event but before it can respond

There are two types of crashes that can lead to this, handling will resolve the ones above and other ones; 
+ The actor itself crashes (likely due to persistence failure)
+ The whole system crashes



## The actor itself crashes
This is fairly easy to resolve, every bank account is started with a supervisor, that monitors it, and in case of crash it resumes  every 5 seconds. As shown here

   ```
   public static Props props(String bankAccountId) {
            return BackoffSupervisor.props(
                    BackoffOpts.onStop(
                            Props.create(BankAccount.class, bankAccountId), bankAccountId,
                            FiniteDuration.create(1, TimeUnit.SECONDS),
                            FiniteDuration.create(5, TimeUnit.SECONDS),
                            0.2)
            );
    }
```
     
Meanwhile the transfer handler retries re-sending the  command 6 times *(configurable)* every 10 seconds *(configurable)*  if the bank account responds before the count down, the normal process resumes, otherwise if is at the first stage of withdrawal it marks as failed and return to the user else it starts a rollback.
    
## The whole system crashes
After 30 minutes the sever started, the system sends a query to the read side to get list of hanging transactions (transaction not marked as completed, failed or rollback) and re-creates their transfer handler, each transfer handler uses its events to build its state and resumes from where it stopped. 
    
  ```
  actorSystem.scheduler().scheduleOnce(Duration.ofMinutes(30), () -> account.walkBackInTime(), actorSystem.dispatcher());
```

Inside walkBackInTime()
   ```
   hangingTransactions.forEach(transactionId -> {
                  actorSystem.actorOf(TransferHandler.props(transactionId, bank), String.format("transaction-%s", transactionId));
   });
```

## Rollback Process

<img  alt="rollback" width="837" src="https://user-images.githubusercontent.com/3046068/65185253-11639d00-da5f-11e9-9e08-291bb3772658.png">   
   
    
Running
===

Under the test folder, there is another package `integration_tests`, it contains the following classes
- `AccountService`: Runs a single scenario of 5 requests, account 4 and 5 getting their balance, then transfer €10 from account 4 to 5, and finally account 4 and 5 getting their new balance and assert that its less and greater than their initial balance respectively
- `MultipleTransferLoad`: Runs `AccountService` above 600 times by creating concurrent 300 users for `AccountService` within 50 seconds and looping twice, resulting in 5 3000 requests
- `ConfirmTransferBalance`: Run a scenario of 2 requests, assert that account 4 has a balance of €4,000 and account 5 has a balance of €16,000
- `CombinedTestSuiteIT`: This is the only directly executable integration test, it runs `MultipleTransferLoad` then `ConfirmTransferBalance` to assert that 600 concurrent requests to transfer €10 each from account 4 to 5 results in balance of €4,000 and €16,000 respectively
  

To package the application without running the integration test 

> mvn clean package

Otherwise you can package and run the integration test
> mvn clean verify

An executable jar file will be produced, then you can run
> java -jar ./target/money-transfer-1.0.jar

Optionally you can run the project through maven
>mvn exec:java -Dexec.mainClass="io.kiamesdavies.revolut.BigBang"

Then you can open ur browser at [localhost:9099](http://localhost:9099/)

Usage
===

talk about the available endpoints

Performance Testing
===

Running the integration test above with generates a [zerocode csv](https://github.com/kiamesdavies/money-transfer/files/3625225/zerocode-junit-granular-report.xlsx) and with of the load template from Igor Vlahek was able to generate the charts below

<img width="886" alt="histo_chart" src="https://user-images.githubusercontent.com/3046068/65130853-27894300-d9f6-11e9-8c20-337395c298d7.png">
<br/>
<br/>
<img width="271" alt="summary" src="https://user-images.githubusercontent.com/3046068/65131158-a7171200-d9f6-11e9-9465-e498c9d02043.png">

<br/>
<br/>
And with the help of Yourkit profiler 
<img width="800" alt="summary" src="https://user-images.githubusercontent.com/3046068/65132829-7684a780-d9f9-11e9-8c84-54c62119dd3d.png">

77 MB out of the allocated 283 MB heap memory was used  for the 3002 requests
<br/>

<img width="800" alt="summary" src="https://user-images.githubusercontent.com/3046068/65132746-5228cb00-d9f9-11e9-8260-28f198019360.png">
Cpu time with a peek of 44%
<br/>
<br/>

<img width="800" alt="summary" src="https://user-images.githubusercontent.com/3046068/65132953-bba8d980-d9f9-11e9-8da9-2b3d7e01ef8a.png">
Thread count peek at 50


Final Thoughts
===

The read side of this application was not written and can easily be implemented, okay maybe I should have said CRS and not CQRS :laughing: , 
<br/>
For a complete production system I would recommend  tagging the events for parallel journal readers, use clustering sharding to distribute the actors and if we want to use a RDBMS for the write side, Citusdata enabled Postgres database can support a digestion of 2.7 billion inserts per day.