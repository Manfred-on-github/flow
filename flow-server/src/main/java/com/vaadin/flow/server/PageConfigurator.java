/*
 * Copyright 2000-2022 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server;

import java.io.Serializable;

import com.vaadin.flow.component.page.AppShellConfigurator;

/**
 * Configures the initial page contents.
 *
 * @since 1.0
 * @deprecated since 3.0 use {@link AppShellConfigurator}
 */
@FunctionalInterface
@Deprecated
public interface PageConfigurator extends Serializable {

    /**
     * Configure the initial page settings when called.
     *
     * @param settings
     *            initial page settings
     * @deprecated Since 3.0, use
     *             {@link AppShellConfigurator#configurePage(AppShellSettings)}
     *             instead
     */
    @Deprecated
    void configurePage(InitialPageSettings settings);
}
