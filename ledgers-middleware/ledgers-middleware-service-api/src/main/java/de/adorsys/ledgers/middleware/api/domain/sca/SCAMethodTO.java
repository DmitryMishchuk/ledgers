/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
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

package de.adorsys.ledgers.middleware.api.domain.sca;

import java.util.Objects;

public class SCAMethodTO {
    private String id;
    private SCAMethodTypeTO type;
    private String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SCAMethodTypeTO getType() {
        return type;
    }

    public void setType(SCAMethodTypeTO type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SCAMethodTO that = (SCAMethodTO) o;
        return Objects.equals(id, that.id) &&
                       type == that.type &&
                       Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, value);
    }

    @Override
    public String toString() {
        return "SCAMethodTO{" +
                       "id='" + id + '\'' +
                       ", type=" + type +
                       ", value='" + value + '\'' +
                       '}';
    }
}


