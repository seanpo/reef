/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.reef.bridge.driver.launch.yarn;

import org.apache.commons.lang.StringUtils;
import org.apache.reef.annotations.audience.Private;
import org.apache.reef.bridge.driver.launch.BridgeDriverLauncher;
import org.apache.reef.bridge.driver.service.DriverServiceConfigurationProvider;
import org.apache.reef.bridge.driver.service.grpc.GRPCDriverServiceConfigurationProvider;
import org.apache.reef.bridge.proto.ClientProtocol;
import org.apache.reef.client.DriverLauncher;
import org.apache.reef.client.LauncherStatus;
import org.apache.reef.runtime.yarn.client.YarnClientConfiguration;
import org.apache.reef.runtime.yarn.driver.parameters.FileSystemUrl;
import org.apache.reef.tang.Configuration;
import org.apache.reef.tang.Configurations;
import org.apache.reef.tang.JavaConfigurationBuilder;
import org.apache.reef.tang.Tang;
import org.apache.reef.tang.exceptions.InjectionException;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a bootstrap launcher for YARN for submission from the bridge. It allows for Java Driver
 * configuration generation directly on the Driver without need of Java dependency if REST
 * submission is used.
 */
@Private
public final class YarnLauncher implements BridgeDriverLauncher {
  private static final Logger LOG = Logger.getLogger(YarnLauncher.class.getName());

  @Inject
  private YarnLauncher() {
  }

  public LauncherStatus launch(final ClientProtocol.DriverClientConfiguration driverClientConfiguration)
      throws InjectionException {
    final DriverServiceConfigurationProvider driverConfigurationProvider =
        Tang.Factory.getTang().newInjector(Tang.Factory.getTang().newConfigurationBuilder()
            .bindImplementation(DriverServiceConfigurationProvider.class,
                GRPCDriverServiceConfigurationProvider.class)
            .build()
        ).getInstance(DriverServiceConfigurationProvider.class);
    Configuration yarnConfiguration = YarnClientConfiguration.CONF
        .set(YarnClientConfiguration.UNMANAGED_DRIVER,
            driverClientConfiguration.getYarnRuntime().getUnmangedDriver())
        .set(YarnClientConfiguration.YARN_PRIORITY, driverClientConfiguration.getYarnRuntime().getPriority())
        .set(YarnClientConfiguration.JVM_HEAP_SLACK, 0.0)
        .build();
    if (StringUtils.isNotEmpty(driverClientConfiguration.getYarnRuntime().getFilesystemUrl())) {
      final JavaConfigurationBuilder providerConfig = Tang.Factory.getTang().newConfigurationBuilder()
          .bindNamedParameter(FileSystemUrl.class, driverClientConfiguration.getYarnRuntime().getFilesystemUrl());
      yarnConfiguration = Configurations.merge(yarnConfiguration, providerConfig.build());
    }
    final LauncherStatus status = DriverLauncher.getLauncher(yarnConfiguration)
        .run(driverConfigurationProvider.getDriverServiceConfiguration(driverClientConfiguration));
    LOG.log(Level.INFO, "Job complete status: {0}", status);
    if (status.getError().isPresent()) {
      LOG.log(Level.SEVERE, "Job completed with error", status.getError().get());
    }
    return status;
  }
}
