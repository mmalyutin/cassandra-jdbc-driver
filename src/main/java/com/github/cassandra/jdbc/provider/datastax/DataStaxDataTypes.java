/*
 *
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
 *
 */
package com.github.cassandra.jdbc.provider.datastax;

import com.datastax.driver.core.LocalDate;
import com.datastax.driver.core.TupleType;
import com.datastax.driver.core.utils.UUIDs;
import com.github.cassandra.jdbc.CassandraDataType;
import com.github.cassandra.jdbc.CassandraDataTypeConverters;
import com.github.cassandra.jdbc.CassandraDataTypeMappings;
import com.google.common.base.Function;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class DataStaxDataTypes extends CassandraDataTypeMappings {
    private static final long EPOCH_MS = Timestamp.valueOf("1970-01-01 00:00:00.000").getTime();

    static final CassandraDataTypeMappings mappings = new CassandraDataTypeMappings() {
        @Override
        protected void init(List<Object[]> list) {
            // http://docs.datastax.com/en/latest-java-driver/java-driver/reference/javaClass2Cql3Datatypes.html
            addMappings(list, CassandraDataType.ASCII.getTypeName(), Types.VARCHAR, String.class,
                    Integer.MAX_VALUE, 0);
            addMappings(list, CassandraDataType.BIGINT.getTypeName(), Types.BIGINT, Long.class, 19, 0);
            addMappings(list, CassandraDataType.BLOB.getTypeName(), Types.BLOB, ByteBuffer.class, Integer.MAX_VALUE, 0);
            addMappings(list, CassandraDataType.BOOLEAN.getTypeName(), Types.BOOLEAN, Boolean.class, 4, 0);
            addMappings(list, CassandraDataType.COUNTER.getTypeName(), Types.BIGINT, Long.class, 19, 0);
            addMappings(list, CassandraDataType.DATE.getTypeName(), Types.DATE, LocalDate.class, 10, 0);
            addMappings(list, CassandraDataType.DECIMAL.getTypeName(), Types.DECIMAL, BigDecimal.class,
                    Integer.MAX_VALUE, 2);
            addMappings(list, CassandraDataType.DOUBLE.getTypeName(), Types.DOUBLE, Double.class, 22, 8);
            addMappings(list, CassandraDataType.FLOAT.getTypeName(), Types.FLOAT, Float.class, 12, 4);
            addMappings(list, CassandraDataType.INET.getTypeName(), Types.VARCHAR, InetAddress.class, 200, 0);
            addMappings(list, CassandraDataType.INT.getTypeName(), Types.INTEGER, Integer.class, 10, 0);
            addMappings(list, CassandraDataType.LIST.getTypeName(), Types.JAVA_OBJECT, List.class,
                    Integer.MAX_VALUE, 0);
            addMappings(list, CassandraDataType.MAP.getTypeName(), Types.JAVA_OBJECT, Map.class, Integer.MAX_VALUE,
                    0);
            addMappings(list, CassandraDataType.SET.getTypeName(), Types.JAVA_OBJECT, Set.class, Integer.MAX_VALUE,
                    0);
            addMappings(list, CassandraDataType.SMALLINT.getTypeName(), Types.SMALLINT, Short.class, 6, 0);
            addMappings(list, CassandraDataType.TEXT.getTypeName(), Types.VARCHAR, String.class, Integer.MAX_VALUE,
                    0);
            addMappings(list, CassandraDataType.TIME.getTypeName(), Types.TIME, Time.class, 50, 0);
            addMappings(list, CassandraDataType.TIMESTAMP.getTypeName(), Types.TIMESTAMP, Timestamp.class, 50, 0);
            addMappings(list, CassandraDataType.TIMEUUID.getTypeName(), Types.VARCHAR, UUID.class, 50, 0);
            addMappings(list, CassandraDataType.TINYINT.getTypeName(), Types.TINYINT, Byte.class, 3, 0);
            addMappings(list, CassandraDataType.TUPLE.getTypeName(), Types.JAVA_OBJECT, TupleType.class,
                    Integer.MAX_VALUE, 0);
            addMappings(list, CassandraDataType.UUID.getTypeName(), Types.VARCHAR, UUID.class, 50, 0);
            addMappings(list, CassandraDataType.VARCHAR.getTypeName(), Types.VARCHAR, String.class,
                    Integer.MAX_VALUE, 0);
            addMappings(list, CassandraDataType.VARINT.getTypeName(), Types.BIGINT, BigInteger.class,
                    Integer.MAX_VALUE, 0);
        }
    };

    static final CassandraDataTypeConverters converters = new CassandraDataTypeConverters() {
        @Override
        protected void init() {
            super.init();

            // add / override converters
            addMapping(LocalDate.class, LocalDate.fromMillisSinceEpoch(System.currentTimeMillis()),
                    new Function<Object, LocalDate>() {
                        public LocalDate apply(Object input) {
                            LocalDate date;
                            if (input instanceof java.util.Date) {
                                date = LocalDate.fromMillisSinceEpoch(((java.util.Date) input).getTime() - EPOCH_MS);
                            } else {
                                date = LocalDate.fromMillisSinceEpoch(
                                        Date.valueOf(String.valueOf(input)).getTime() - EPOCH_MS);
                            }
                            return date;
                        }
                    });
            // Use DataStax UUIDs to generate time-based UUID
            addMapping(java.util.UUID.class, UUIDs.timeBased(), new Function<Object, UUID>() {
                public UUID apply(Object input) {
                    return java.util.UUID.fromString(String.valueOf(input));
                }
            });
            // workaround for Date, Time and Timestamp
            addMapping(Date.class, new Date(System.currentTimeMillis()),
                    new Function<Object, Date>() {
                        public Date apply(Object input) {
                            Date date;
                            if (input instanceof LocalDate) {
                                date = new Date(((LocalDate) input).getMillisSinceEpoch() + EPOCH_MS);
                            } else if (input instanceof java.util.Date) {
                                date = new Date(((java.util.Date) input).getTime());
                            } else if (input instanceof Number) {
                                date = new Date(((Number) input).longValue());
                            } else {
                                date = Date.valueOf(String.valueOf(input));
                            }
                            return date;
                        }
                    });
            addMapping(Time.class, new Time(System.currentTimeMillis()),
                    new Function<Object, Time>() {
                        public Time apply(Object input) {
                            Time time;
                            if (input instanceof LocalDate) {
                                time = new Time(((LocalDate) input).getMillisSinceEpoch() + EPOCH_MS);
                            } else if (input instanceof java.util.Date) {
                                time = new Time(((java.util.Date) input).getTime());
                            } else if (input instanceof Number) {
                                time = new Time(((Number) input).longValue());
                            } else {
                                time = new Time(Time.valueOf(String.valueOf(input)).getTime());
                            }
                            return time;
                        }
                    });
            addMapping(Timestamp.class, new Timestamp(System.currentTimeMillis()),
                    new Function<Object, Timestamp>() {
                        public Timestamp apply(Object input) {
                            Timestamp timestamp;
                            if (input instanceof LocalDate) {
                                timestamp = new Timestamp(((LocalDate) input).getMillisSinceEpoch() + EPOCH_MS);
                            } else if (input instanceof java.util.Date) {
                                timestamp = new Timestamp(((java.util.Date) input).getTime());
                            } else if (input instanceof Number) {
                                timestamp = new Timestamp(((Number) input).longValue());
                            } else {
                                timestamp = Timestamp.valueOf(String.valueOf(input));
                            }
                            return timestamp;
                        }
                    });
        }
    };
}