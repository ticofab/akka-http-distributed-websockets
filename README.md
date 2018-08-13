# Distributed Websockets with Akka Http

This repository goes with the blog post [LINK](). It shows one way to manage websocket connections with remote actors.

## Usage

Download the repository. First start the listener app with `sbt run` in the `listener` directory.

Then, start one or more node handlers: `sbt -DPORT=1234 run`. If omitted, the default port is `2552`. Note that for simplicity, each node creates ONE actor and registers with the listener. This code is just meant as an example - for more serious use, you should create some kind of hierarchy and make sure that every new connection is handled by a different actor! 

The listener accepts incoming websocket connections at `ws://127.0.0.1:8080/connect`. Once the connection has been opened, the handling actor will reply with a greeting to any incoming message.

## License

    Copyright 2018 Fabio Tiriticco / Fabway

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

