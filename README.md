# core3 - Example Workflow Engine

Example Scala Play 2.6 app showcasing a way to build a backend service with [core3](https://github.com/Interel-Group/core3).

## Getting Started
* Clone repo
* Get Redis ([example setup](https://github.com/Interel-Group/core3/wiki/Example-Redis-Setup-(Ubuntu)))
* Add ```static.conf``` (see the [reference config](conf/static_ref.conf) for more info)
* ```sbt run -Dhttps.port=<some local port> -Dhttp.port=disabled -Dc3ee.console=enabled```

## Supported data sources
* [Redis](https://redis.io/) (tested on 3.2.5, 3.2.8)

## Supported auth providers
* [Local](https://github.com/Interel-Group/core3/wiki) - local credentials DB

## Deployment

[Deploying a Play 2.6 application](https://www.playframework.com/documentation/2.6.x/Production)

## Testing
Only one test is provided and it is used to create a few test users. See the [spec](test/core3_example_engine/test/ExampleInitSpec.scala) for details.

```
sbt test
```
Required options:
```
-Dserver.static.database.redis.secret=<some password>
```

## Interesting files
* [app/controllers/Service](app/controllers/Service.scala) - example of client and user -aware actions
* [app/Module](app/Module.scala) - component setup
* [conf/routes](conf/routes) - routing config
* [app/ConsoleStart](app/ConsoleStart.scala) - enables the system management console

## Built With
* Scala 2.12.3
* sbt 0.13.16
* [core3](https://github.com/Interel-Group/core3) - Core framework
* [rediscala](https://github.com/etaty/rediscala) - Redis data layer support

## Versioning
We use [SemVer](http://semver.org/) for versioning.

## License
This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details

> Copyright 2017 Interel
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this file except in compliance with the License.
> You may obtain a copy of the License at
>
> http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
