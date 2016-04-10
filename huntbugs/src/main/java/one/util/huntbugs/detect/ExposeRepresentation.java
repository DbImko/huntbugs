/*
 * Copyright 2015, 2016 Tagir Valeev
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
package one.util.huntbugs.detect;

import com.strobel.assembler.metadata.FieldDefinition;
import com.strobel.assembler.metadata.FieldReference;
import com.strobel.assembler.metadata.MethodDefinition;
import com.strobel.assembler.metadata.ParameterDefinition;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.assembler.metadata.VariableDefinition;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.Expression;
import com.strobel.decompiler.ast.Variable;

import one.util.huntbugs.registry.MethodContext;
import one.util.huntbugs.registry.anno.AstNodes;
import one.util.huntbugs.registry.anno.AstVisitor;
import one.util.huntbugs.registry.anno.MethodVisitor;
import one.util.huntbugs.registry.anno.WarningDefinition;
import one.util.huntbugs.util.Nodes;

/**
 * @author lan
 *
 */
@WarningDefinition(category="MaliciousCode", name="ExposeMutableFieldViaParameter", maxScore=45)
@WarningDefinition(category="MaliciousCode", name="ExposeMutableStaticFieldViaParameter", maxScore=55)
public class ExposeRepresentation {
    @MethodVisitor
    public boolean checkMethod(MethodDefinition md, TypeDefinition td) {
        return td.isPublic() && (md.isPublic() || md.isProtected()) && !md.getParameters().isEmpty();
    }
    
    @AstVisitor(nodes=AstNodes.EXPRESSIONS)
    public void visit(Expression expr, MethodContext mc, MethodDefinition md) {
        if(!md.isStatic() && expr.getCode() == AstCode.PutField) {
            FieldDefinition fd = ((FieldReference) expr.getOperand()).resolve();
            if(fd != null && (fd.isPrivate() || fd.isPackagePrivate() || fd.isProtected())) {
                if(md.isProtected() && fd.isProtected())
                    return;
                if(!isMutable(fd.getFieldType()))
                    return;
                Expression self = Nodes.getChild(expr, 0);
                if(!isThis(self))
                    return;
                Expression value = Nodes.getChild(expr, 1);
                if(!(value.getOperand() instanceof ParameterDefinition))
                    return;
                ParameterDefinition pd = (ParameterDefinition)value.getOperand();
                int priority = 0;
                if(md.isProtected() || fd.isProtected())
                    priority+=10;
                if(md.isVarArgs() && pd.getPosition() == md.getParameters().size()-1)
                    priority+=10;
                mc.report("ExposeMutableFieldViaParameter", priority, expr);
            }                    
        }
        if(expr.getCode() == AstCode.PutStatic) {
            FieldDefinition fd = ((FieldReference) expr.getOperand()).resolve();
            if(fd != null && (fd.isPrivate() || fd.isPackagePrivate())) {
                if(!isMutable(fd.getFieldType()))
                    return;
                Expression value = Nodes.getChild(expr, 0);
                if(!(value.getOperand() instanceof ParameterDefinition))
                    return;
                ParameterDefinition pd = (ParameterDefinition)value.getOperand();
                int priority = 0;
                if(md.isProtected())
                    priority+=10;
                if(md.isVarArgs() && pd.getPosition() == md.getParameters().size()-1)
                    priority+=10;
                mc.report("ExposeMutableStaticFieldViaParameter", priority, expr);
            }                    
        }
    }

    private boolean isThis(Expression self) {
        if(self.getCode() == AstCode.Load && self.getOperand() instanceof Variable) {
            VariableDefinition origVar = ((Variable)self.getOperand()).getOriginalVariable();
            return origVar != null && origVar.getSlot() == 0;
        }
        return false;
    }

    private boolean isMutable(TypeReference fieldType) {
        if(fieldType.isArray())
            return true;
        String typeName = fieldType.getInternalName();
        if(typeName.equals("java/util/Hashtable") ||
                typeName.equals("java/util/Vector") ||
                typeName.equals("java/util/Date") || 
                typeName.equals("java/sql/Date") || 
                typeName.equals("java/sql/Timestamp"))
            return true;
        return false;
    }
}
