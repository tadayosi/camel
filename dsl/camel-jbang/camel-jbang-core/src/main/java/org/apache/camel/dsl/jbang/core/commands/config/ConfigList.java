/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.dsl.jbang.core.commands.config;

import org.apache.camel.dsl.jbang.core.commands.CamelCommand;
import org.apache.camel.dsl.jbang.core.commands.CamelJBangMain;
import org.apache.camel.dsl.jbang.core.common.CommandLineHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "list", description = "Displays user configuration", sortOptions = false, showDefaultValues = true)
public class ConfigList extends CamelCommand {

    @CommandLine.Option(names = { "--global" }, description = "Use global or local configuration")
    boolean global = true;

    public ConfigList(CamelJBangMain main) {
        super(main);
    }

    @Override
    public Integer doCall() throws Exception {
        listConfigurations(true);
        if (!global) {
            return 0;
        }

        listConfigurations(false);
        return 0;
    }

    private void listConfigurations(boolean local) {
        CommandLineHelper
                .loadProperties(p -> {
                    if (!p.stringPropertyNames().isEmpty()) {
                        String configurationType = local ? "Local" : "Global";
                        printer().println("----- " + configurationType + " -----");
                    }
                    for (String k : p.stringPropertyNames()) {
                        String v = p.getProperty(k);
                        printer().printf("%s = %s%n", k, v);
                    }
                }, local);
    }
}
