package com.siberika.idea.pascal.lang.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.SmartPsiElementPointer;
import com.siberika.idea.pascal.lang.psi.PasEntityScope;
import com.siberika.idea.pascal.lang.psi.PasInheritedCall;
import com.siberika.idea.pascal.lang.psi.PascalRoutine;
import com.siberika.idea.pascal.lang.psi.PascalStructType;
import com.siberika.idea.pascal.lang.psi.impl.PasField;
import com.siberika.idea.pascal.lang.search.routine.ParamCountRoutineMatcher;
import com.siberika.idea.pascal.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

class PascalInheritedReference extends PsiReferenceBase<PasInheritedCall> {

    PascalInheritedReference(@NotNull PsiElement element) {
        super((PasInheritedCall) element, TextRange.from(element.getStartOffsetInParent(), element.getTextLength()));
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        AtomicReference<PsiElement> result = new AtomicReference<>();
        final PasEntityScope method = PsiUtil.getNearestAffectingScope(myElement);
        if (method instanceof PascalRoutine) {
            final ParamCountRoutineMatcher matcher = new ParamCountRoutineMatcher(method.getNamePart(), ((PascalRoutine) method).getFormalParameterNames().size()) {
                @Override
                protected boolean onMatch(final PasField field, final PascalRoutine routine) {
                    result.set(routine);
                    return false;
                }
            };
            findInheritedMethod(getParentScope(method.getContainingScope()), matcher);
        }
        return result.get();
    }

    private void findInheritedMethod(final PasEntityScope parentScope, final ParamCountRoutineMatcher matcher) {
        if ((parentScope != null) && matcher.process(parentScope.getAllFields())) {
            findInheritedMethod(getParentScope(parentScope), matcher);
        }
    }

    private PasEntityScope getParentScope(final PasEntityScope scope) {
        if (scope instanceof PascalStructType) {
            final List<SmartPsiElementPointer<PasEntityScope>> parents = scope.getParentScope();
            return parents.isEmpty() ? null : parents.iterator().next().getElement();
        } else {
            return null;
        }
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return EMPTY_ARRAY;
    }

}
