#Distributed Message Passing System

## Getting Started
It is recommended to run this project in IntelliJ. When starting up this project in IntelliJ, directories need to be labeled properly. To do this, do the following:

Right click on ***src/main/scala*** and select **Mark Directory as -> Sources Root**. Similarly, under **src**, mark the test directory as **Test Sources Root**. One last thing: under ***src/multi-jvm*** **unmark scala as Test Sources Root**. This needs to to unmarked because when running tests, IntelliJ will try to run the integration tests which should only be ran via SBT.

## Running the Project

To run the project, make sure that all dependencies have been downloaded by SBT. Then if you are using IntelliJ, right-click ***src/main/scala/ClusterApp*** and select **Run ClusterApp**. Make sure to have ***src/main/scala*** marked as **Source Root** before you do this!

Now that the cluster is running, the fastest way to present the data (other than the cluster logs) is to donwload the [Dark WebSocket Terminal Chrome Extension](https://chrome.google.com/webstore/detail/dark-websocket-terminal/dmogdjmcpfaibncngoolgljgocdabhke?hl=en). Once you have this chrome extension downloaded, run it and in the Dark WebSocket Terminal, enter the following command (when running in a dev environment):

    /connect ws://localhost:9000/ws/workers-exchange?nodeId=1

The **nodeId** GET parameter specifies which PI node you wish to monitor. If the node does not exist, you will not get a connection. By default, there are three nodes, so nodeId 1,2,3 should all work.

You can also check to see if the server is alive and willing to work with WebSockets by connecting to the following route:

    /connect ws://localhost:9000/ws/echo

##Integration Tests:
In [Akka](http://akka.io/), integration tests are tests that are ran on multiple JVMs to simulate multiple nodes in a cluster. All integration tests must be under `src/main/multi-jvm` and often need their own configuration.

####IntelliJ Setup
Go to **Run->Edit Configurations** and add a new SBT task. Call it whatever you want. Under **Tasks**, enter **multi-jvm:test**. You should now be able to run this task to run integration tests.

####Standard Setup
Install [SBT](http://www.scala-sbt.org/) and execute the following from the SBT command line:

`> multi-jvm:test
`

##Unit tests:
If you are using IntelliJ, mark the test directory as *"Test Sources Root"* and run the tests just like you would for any other scala test.

##Serialization
Protobufs ([Google protocol buffers](https://developers.google.com/protocol-buffers/)) were used so that data models can be sent as messages between different platforms. In order to run the project and tests properly, the protobufs need to be compiled.

[ScalaPB](https://github.com/trueaccord/ScalaPB) is used to compile the protobufs into Scala classes and objects as protobufs do not natively support Scala. You can find this dependency in `project/plugins.sbt`.

###How do I compile my protobufs in Scala?

If you do not have SBT installed on your system, go [download](http://www.scala-sbt.org/) it. Once you have SBT installed, run the following on the SBT command line:

    > protobuf:protobufGenerate


Once compiled, the generated models should appear under `src/main/generated`

***Note:*** It is important to notice that this command will not work on the SBT that ships with IntelliJ because certain classes depend on generated protobuf models and IntelliJ will complain that there are missing classes (thus not running the compile task).