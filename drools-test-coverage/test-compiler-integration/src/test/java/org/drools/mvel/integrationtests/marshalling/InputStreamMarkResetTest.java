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
package org.drools.mvel.integrationtests.marshalling;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Should Drools ever need it, this test shows who we can implement peak using a {@link BufferedInputStream} 
 * in combination with an {@link ObjectInputStream}.
 * 
 * If you come across this and want to delete it, go ahead. Hopefully we'll never need the code. 
 */
public class InputStreamMarkResetTest {

    @Test
    public void basicReadAndWriteObjectTest() throws Exception { 

        DataObject thingy = new DataObject();
        thingy.type = 'a';
        thingy.time = new Date().getTime();
        thingy.strCount = "1";

        byte [] bytes = marshallThingy(thingy);

        
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        BufferedInputStream bis = new BufferedInputStream(bais);
        ObjectInputStream stream = new ObjectInputStream(bis);

        DataObject unmaThingy = new DataObject();
        unmaThingy.type = stream.readChar();
        unmaThingy.time = stream.readLong();

        assertThat(bis.markSupported()).as("Mark/reset is not supported").isTrue();
        bis.mark(8);
        int [] intBytes = new int [4];
        intBytes[0] = bis.read();
        intBytes[1] = bis.read();
        intBytes[2] = bis.read();
        intBytes[3] = bis.read();
        if ((intBytes[0] | intBytes[1] | intBytes[2] | intBytes[3] ) < 0) { 
            bis.reset();
        }
        unmaThingy.strCount = stream.readUTF();

        assertThat(thingy.equals(unmaThingy)).isTrue();
    }

    private byte [] marshallThingy(DataObject thingy ) throws IOException { 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(baos);

        stream.writeChar(thingy.type);
        stream.writeLong(thingy.time);
        if( thingy.intCount != null ) { 
            stream.writeInt(thingy.intCount);
        }
        else if( thingy.strCount != null ) { 
            stream.writeUTF(thingy.strCount);
        }
        else { 
            throw new IllegalStateException("Neither the integer nor String count is filled." );
        }
        stream.close();

        return baos.toByteArray();
    }

    private class DataObject {
        char type;
        long time;

        // one or the other..
        Integer intCount;
        String strCount;

        @Override
        public boolean equals(Object obj) { 
            if( obj == null ) { 
                return false;
            }
            if( ! this.getClass().equals(obj.getClass()) ) {
                return false;
            }
            else { 
                DataObject newThingy = (DataObject) obj;
                if( this.type != newThingy.type ) { 
                    return false;
                }
                if( this.time != newThingy.time ) { 
                    return false;
                }
                Integer count;
                if( this.intCount != null ) { 
                    count = this.intCount;
                }
                else {
                    count = Integer.parseInt(this.strCount);
                }
                Integer newCount;
                if( newThingy.intCount != null ) { 
                    newCount = newThingy.intCount; 
                }
                else { 
                    newCount = Integer.parseInt(newThingy.strCount);
                }
                if( count != null && newCount != null ) { 
                    if( ! count.equals(newCount) ) { 
                        return false;
                    }
                }
                else if(!Objects.equals(count, newCount)) {
                    return false;
                }
            }
            return true;
        }
    }
}
