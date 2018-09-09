# smack-client
Tool that can be used to test the smack system.

## Usage
```
smack-client 0.3.0-SNAPSHOT
Usage: smack-client [options]

  -c, --count <num>        The number of requests to do before stop (0 for infinite)
  -d, --debug <bool>       True if debug should be enabled
  -h, --host <addr>        The host of the service to test (default: localhost)
  -l, --loglevel <level>   The log level used by standard output and (optionally) by sentry logger (default: info)
  -r, --parallelism <num>  The number of actors to start (default: 2)
  -p, --port <port>        The port of the service to test (default: 80)
  -n, --requests-per-second <num>
                           The number of requests per second that each actor should send (default: 20)
  --sentry-dns <key>       If defined, every logs are sent to sentry servers and can be viewed on Sentry.io. The standard output remain unchanged
  --help                   Display help
Tool that can be used to stress test the smack system.
```

An example of a script to launch the command is available here: [run-client](/scripts/run-client).

## Behavior
When the client starts, a number of actors equals to _parallelism_ are created.
Each actor contains a queue in which every second _requests-per-second_ requests are enqueued.
Each request is made to the address `http://$host:$port/$uri`. The client processes only the requests that it succeeds in making, the others it discards.
Every 5 seconds the partial statistics are printed, which contain:
* The number of answers obtained, grouped by the response code
* The number of requests entered in the request queue
* The number of requests discarded from the request queue (queue overflow)
* The number of failed requests
* The total number of requests
* The minimum time, in milliseconds, between request and response
* The maximum time, in milliseconds, between request and response
* The average time, in milliseconds, between request and response

At the end of the client execution, when _count_ requests have been made or when the program is forcibly interrupted, global statistics are printed.

The detail of each request is defined within the `Worker`.

## Structure
To create a new one `Worker` use the [LogWorker](/client/src/main/scala/smack/client/LogWorker.scala) as an example.
A worker must be an actor that is initialized in the [WebClient](/client/src/main/scala/smack/client/WebClient.scala).
The worker, by the constructor, shall receive the following parameters: `host: String, port: Int, requestsPerSecond: Int`.
Internally it must create a request queue, define the type of request to be made
and start the scheduler that sends to the same actor an object `ExecuteRequest(request: HttpRequest)` each time a request has to be made.
When the message `GetStatistics` is received from the parent actor (`WebClient`), the worker must reply with a message
`Statistics(responses: Map[Int, Long], enqueued: Long, dropped: Long, failed: Long, bt: BenchTimes)` containing the local partial statistics.

Note: this tool was not the aim of the main project and was quickly implemented without designing extensibility criteria.
It should therefore be improved because it now has several problems and is hardly extensible.
