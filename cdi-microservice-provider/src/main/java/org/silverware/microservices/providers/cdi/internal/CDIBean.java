/*
 * -----------------------------------------------------------------------\
 * SilverWare
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.silverware.microservices.providers.cdi.internal;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

/**
 * CDI Bean instance.
 *
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class CDIBean {

    private Bean _bean;
    private BeanManager _beanManager;

    /**
     * Public constructor.
     * @param bean The bean isntance.
     * @param beanManager The bean manager.
     */
    public CDIBean(Bean bean, BeanManager beanManager) {
        this._bean = bean;
        this._beanManager = beanManager;
    }

    /**
     * Get the CDI Bean instance.
     * @return The CDI bean instance.
     */
    public Bean getBean() {
        return _bean;
    }

    /**
     * Get the CDI BeanManager instance.
     * @return The CDI BeanManager instance.
     */
    public BeanManager getBeanManager() {
        return _beanManager;
    }
}