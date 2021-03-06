/*
 * Copyright 2016 HuntBugs contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.util.huntbugs.testdata;

import java.util.Hashtable;

import one.util.huntbugs.registry.anno.AssertNoWarning;
import one.util.huntbugs.registry.anno.AssertWarning;

/**
 * @author Tagir Valeev
 *
 */
public class TestExposeRepresentation {
    private class InternalClass {
        private int[] f;
        
        @AssertNoWarning("*")
        public void setField(int[] f) {
            this.f = f;
        }
    }
    
    int[] f;
    static Hashtable<String, Integer> ht;
    InternalClass ic = new InternalClass();
    Point p;

    @AssertWarning(value="ExposeMutableFieldViaParameter", minScore=30)
    public void setField(int[] f) {
        if(f.length > 2)
            this.f = f;
        if(ic.f == null)
            ic.setField(f);
    }
    
    @AssertNoWarning("ExposeMutableFieldViaParameter")
    public void setFieldClone(int[] f) {
        f = f.clone();
        this.f = f;
    }
    
    @AssertWarning(value="ExposeMutableFieldViaParameter", maxScore=29)
    public void setFieldVarArgs(int... f) {
        this.f = f;
    }
    
    @AssertNoWarning("ExposeMutableFieldViaParameter")
    public void setField(TestExposeRepresentation obj, int[] f) {
        obj.f = f;
    }
    
    @AssertWarning(value="ExposeMutableStaticFieldViaParameter", minScore=45)
    public static void setHashTable(Hashtable<String, Integer> ht) {
        TestExposeRepresentation.ht = ht;
        System.out.println(ht);
    }
    
    @AssertWarning("ExposeMutableFieldViaParameter")
    public void setPoint(Point p) {
        this.p = p;
    }
    
    public class Point {
        public int x, y;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
    }
}
