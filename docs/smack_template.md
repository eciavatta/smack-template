# smack-template
smack-template is a case study of an application built with the SMACK stack.

## Usage
```
smack-template 0.3.0-SNAPSHOT
Usage: smack-template [options] <role>

  -a, --akka-seeds <addr>  The akka seed node/s
  -c, --cassandra-contact-points <addr>
                           The cassandra contact point/s (default: 127.0.0.1:9042)
  --datadog-agent <addr>   The address of the Datadog agent (default: 127.0.0.1:8126)
  --datadog-api <key>      The Datadog api key
  -d, --debug <bool>       True if debug should be enabled
  -e, --environment <env>  The environment to be used (default: development)
  -k, --kafka-bootstraps <addr>
                           The kafka bootstrap server/s (default: 127.0.0.1:9092)
  -l, --loglevel <level>   The log level used by standard output and (optionally) by sentry logger (default: info)
  --sentry-dns <key>       If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged
  <role>                   The role of the instance to start
  --help                   Display help
Run the instance specified by role and connect to the cluster.
```

## Behavior
The application consists of four parts (or roles), *backend*, *frontend*, *service* e *seed*.
Each role contributes to a part of the system and at least one instance of all roles is required to run the application.
Each role is independent of the others and a 1: 1 relationship is not required between the instances.
It is therefore possible, for example, to scale only the part of the application which is subject to greater load.
The function of each role is explained in detail below.

### Backend
It is the role that collects the requests of users sent to the frontend and turn them into responses.
The requests received are sent from the frontend in the form of messages containing the type of action to be performed and the content of the request.
The backend has a supervisor that intercepts the type of action to be performed by recognizing the class of the object that contains the message,
and forwards it to the controller that must handle that type of action.
The controller has the task of checking first if the request is valid;
otherwise an error message must be sent to the frontend that takes care of responding to the client who made the request.
For valid requests, the controller can choose from the following options:
* Manage the request locally and perform the necessary computation, responding immediately to the frontend with the results
* Send a new event to a Kafka broker if it is not necessary to process the request immediately and notify the frontend of the taking charge
* Permanently save the data received in the Cassandra cluster and provide the frontend with the references of the transaction

In case of an error in the controller, the frontend is alerted to the problem and the controller is restarted.
The connections with the Kafka brokers and with the Cassandra nodes are then re-established.

### Frontend
It starts an instance of a web server that takes care of collecting client requests and providing them with an answer.
The web server can be exposed directly outside the cluster or it can be connected to a hardware or software load balancer like
[HAProxy](http://www.haproxy.org/).
The frontend does not handle requests directly but forward them to the backend. If multiple backend are available within the cluster,
the request is sent to the node that has less workload, which reduces processing time.
The web server can handle multiple requests concurrently. The REST architecture was used to implement the application interaction with users.
Each resource must be to declare a path (route) and a function that constructs the message with the request to be sent to the backend.
At the reception of a request, the web server selects the corresponding route for the desired resource: if the route exists the request will be processed,
otherwise an error response is generated.
If there are no active backend, there are but they are congested or the backend stops for a unexpected exception, an error response is generated.
If the frontend is started in debug mode the details of the error are shown within the response JSON object.
Within the frontend is implemented a simple validation of the requests before they are forwarded to the backend, so as to immediately reject invalid requests.

### Service
It is the service that deals with processing requests that are not immediately processed by the backend.
It is used, for example, to consume events from Kafka brokers, process them and save them permanently in the Cassandra cluster.
The service role has a supervisor that monitors the status of active services.
When an unexpected error occurs in a service, the supervisor has the task of restarting the service and re-establishing connections with external services.

### Seed
It is the role that deals with joining the new nodes with the already existing nodes that form the cluster.
Each node in the cluster, at startup, must specify the address of a seed. You can use the seed role to do this.
The first seed node started in a new cluster must join with itself, so it must use as `akka-seeds` argument its own address or domain name.

## Structure

### Backend
The backend has the task of processing the messages sent from the frontend. The messages are exchanged via the network and must be serializable.
The [Protocol Buffers](https://developers.google.com/protocol-buffers/) library developed by Google is used for object serialization.
The message definitions can be inserted into [messages.proto](/commons/src/main/protobuf/messages.proto) inside the commons module,
while the structure definitions can be inserted into [structures.proto](/commons/src/main/protobuf/structures.proto).
The defined classes are autogenerated at the time of compilation.
To serialize and deserialize objects correctly, you must create a manifest for each class,
to be defined respectively within the [MessageSerializer](/commons/src/main/scala/smack/commons/serialization/MessageSerializer.scala)
and [StructureSerializer](/commons/src/main/scala/smack/commons/serialization/StructureSerializer.scala) classes, always inside the commons module.
Finally you need to declare inside the configuration file [serialization.conf](/commons/src/main/resources/serialization.conf)
who is responsible for serializing and deserializing a given class.
For the documentation and the syntax to use for defining serializable classes, refer to the
[Language Guide](https://developers.google.com/protocol-buffers/docs/proto3).

Once the messages have been defined, a new controller can be created by extending the base interface `Controller`.
If the controller needs to interact with Kafka brokers, the extension `KafkaController` is available where the method
`sendToKafka(message: AnyRef): Future[Done]` can be used to send events to Kafka partitions of a given topic, defined by `kafkaTopic`.
To permanently save data or to select it, you can extend the trait `CassandraController` that contains the method
`executeQuery(cassandraMessage: CassandraMessage): Future[ResultSet]` for executing queries and for obtaining results.

The controller must be an actor that must be created in the [BackendSupervisor](/src/main/scala/smack/backend/BackendSupervisor.scala).
Again in the supervisor you must declare that the messages relating to the controller that has been created must be forwarded to the controller itself.
The controller has the task of transforming the requests (originally sent from the frontend in the form of messages) into replies.
Each response must encapsulate the object `ResponseStatus` that contains the HTTP response code, an optional message,
and the stack trace in case of an exception.
Within the trait `Controller` there are already available methods for creating default answers,
such as `success()`, `created()`, `accepted()`, `notFound()`, `badRequest(message)`, `serviceUnavailable()`, `internalServerError(throwable, message)`.
In the case of an exception the method `responseRecovery(throwable): Option[ResponseStatus]` is provided for creating the object `ResponseStatus` from the
exception type. An example of a controller that extends `KafkaController` is [LogController](/src/main/scala/smack/backend/controllers/LogController.scala),
while a controller that extends `CassandraController` is [UserController](/src/main/scala/smack/backend/controllers/UserController.scala).

### Frontend
The frontend contains a set of routes that are used to define the interface of the REST APIs.
The routes must be contained within the package [routes](/src/main/scala/smack/frontend/routes).
Each route must extend the class `RestRoute` that requires to define the backend router and the system actor configuration.
It also contains the method `handle` that facilitates the sending of requests to the backend and manages the responses forwarding them to the client.
Requests sent to the backend must be serializable messages, as explained in the backend paragraph.
With the client, the frontend can exchange objects in JSON format.
It is therefore necessary to implement for each class to be converted in JSON a implicit converter that can be inserted into the package
[marshallers](/commons/src/main/scala/smack/commons/mashallers) in the common module.
For documentation on (de)serialization in JSON you can refer to the library that deals with it, ie [spray-json](https://github.com/spray/spray-json).
Each route must also implement the method `route: Route` that defines the route itself;
it must be a directive that follows the rules and the syntax of the library that implements them,
that is [Akka Http](https://doc.akka.io/docs/akka-http/current/index.html).
Please refer to the documentation on the [Directives](https://doc.akka.io/docs/akka-http/current/routing-dsl/directives/index.html).

Requests can be validated before being forwarded to the backend.
In particular, a validation system is implemented that checks whether the fields of JSON objects sent by the client, and then transformed into Java objects,
satisfy certain criteria. You can insert new rules within the package [validation](/src/main/scala/smack/frontend/validation).
Each validation rule must be an object that implements the method `apply`,
which has as parameters the name of the field of the object to be validated (`fieldName: String`) and optionally other parameters.
The method must return an object `FieldRule`, which contains the function that is used for field validation
and the error message to be returned to the client in case of validation failure.
Examples of validation rules can be found in [ValidationRules](/src/main/scala/smack/frontend/validation/ValidationRules.scala).
A complete example showing the validation of the model and the implementation of some routes can be found in
[UserRoute](/src/main/scala/smack/frontend/routes/UserRoute.scala).

### Service
Service is a set of services that are monitored by a supervisor.
Each service can be implemented using an actor and inserted in the package [services](/src/main/scala/smack/backend/services).
The service actor must be created within the [ServiceSupervisor](/src/main/scala/smack/backend/ServiceSupervisor.scala).
An example of a service is available in [LogService](/src/main/scala/smack/backend/services/LogService.scala).
