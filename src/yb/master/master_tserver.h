// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//

#ifndef YB_MASTER_MASTER_TSERVER_H
#define YB_MASTER_MASTER_TSERVER_H

#include "yb/tserver/tablet_server_interface.h"
#include "yb/util/metrics.h"

namespace yb {
namespace master {

// Master's version of a TabletServer which is required to support virtual tables in the Master.
// This isn't really an actual server and is just a nice way of overriding the default tablet
// server interface to support virtual tables.
class MasterTabletServer : public tserver::TabletServerIf {
 public:
  explicit MasterTabletServer(scoped_refptr<MetricEntity> metric_entity);
  tserver::TSTabletManager* tablet_manager() override;

  server::Clock* Clock() override;
  const scoped_refptr<MetricEntity>& MetricEnt() const override;

 private:
  std::unique_ptr<MetricRegistry> metric_registry_;
  scoped_refptr<MetricEntity> metric_entity_;
};

} // namespace master
} // namespace yb
#endif // YB_MASTER_MASTER_TSERVER_H
