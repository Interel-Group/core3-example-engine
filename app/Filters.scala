/**
  * Copyright 2017 Interel
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
import javax.inject._

import akka.stream.Materializer
import core3.http.filters.{CompressionFilter, MaintenanceModeFilter, MetricsFilter, TraceFilter}
import play.api._
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.cors.CORSFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import play.filters.hosts.AllowedHostsFilter

@Singleton
class Filters @Inject()(metrics: MetricsFilter,
                        compress: CompressionFilter,
                        trace: TraceFilter,
                        maintenance: MaintenanceModeFilter,
                        hosts: AllowedHostsFilter,
                        security: SecurityHeadersFilter,
                        cors: CORSFilter,
                        environment: Environment)
                       (implicit mat: Materializer) extends HttpFilters {

  override val filters: Seq[EssentialFilter] = {
    environment.mode match {
      case Mode.Dev => Seq(metrics, trace, maintenance, security, hosts, cors, compress)
      case Mode.Test => Seq(metrics, trace, maintenance, security, hosts, cors, compress)
      case Mode.Prod => Seq(metrics, trace, maintenance, security, hosts, cors, compress)
    }
  }
}
