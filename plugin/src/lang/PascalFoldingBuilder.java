package com.siberika.idea.pascal.lang;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.lang.folding.NamedFoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.siberika.idea.pascal.lang.psi.PasCaseStatement;
import com.siberika.idea.pascal.lang.psi.PasClassHelperDecl;
import com.siberika.idea.pascal.lang.psi.PasClassTypeDecl;
import com.siberika.idea.pascal.lang.psi.PasClassTypeTypeDecl;
import com.siberika.idea.pascal.lang.psi.PasCompoundStatement;
import com.siberika.idea.pascal.lang.psi.PasConstSection;
import com.siberika.idea.pascal.lang.psi.PasEnumType;
import com.siberika.idea.pascal.lang.psi.PasHandler;
import com.siberika.idea.pascal.lang.psi.PasInterfaceTypeDecl;
import com.siberika.idea.pascal.lang.psi.PasNamedIdentDecl;
import com.siberika.idea.pascal.lang.psi.PasObjectDecl;
import com.siberika.idea.pascal.lang.psi.PasRecordDecl;
import com.siberika.idea.pascal.lang.psi.PasRecordHelperDecl;
import com.siberika.idea.pascal.lang.psi.PasRepeatStatement;
import com.siberika.idea.pascal.lang.psi.PasTypeDeclaration;
import com.siberika.idea.pascal.lang.psi.PasTypeSection;
import com.siberika.idea.pascal.lang.psi.PasTypes;
import com.siberika.idea.pascal.lang.psi.PasUnitFinalization;
import com.siberika.idea.pascal.lang.psi.PasUnitImplementation;
import com.siberika.idea.pascal.lang.psi.PasUnitInitialization;
import com.siberika.idea.pascal.lang.psi.PasUnitInterface;
import com.siberika.idea.pascal.lang.psi.PasUsesClause;
import com.siberika.idea.pascal.lang.psi.PasVarSection;
import com.siberika.idea.pascal.lang.psi.PascalPsiElement;
import com.siberika.idea.pascal.lang.psi.PascalQualifiedIdent;
import com.siberika.idea.pascal.lang.psi.impl.PasRoutineImplDeclImpl;
import com.siberika.idea.pascal.util.PsiUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Author: George Bakhtadze
 * Date: 24/03/2013
 */
public class PascalFoldingBuilder extends FoldingBuilderEx {

    private static final TokenSet TOKENS_COLLAPSED = TokenSet.create(PasTypes.COMMENT, PasTypes.USES_CLAUSE);

    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        final List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();

        Collection<PascalPsiElement> commonElements = new ArrayList<>();
        Collection<PasCaseStatement> caseElements = new ArrayList<>();
        Collection<PasUsesClause> usesElements = new ArrayList<>();
        Collection<PasEnumType> enumElements = new ArrayList<>();
        Collection<PasRoutineImplDeclImpl> routineElements = new ArrayList<>();
        Collection<PsiComment> commentElements = new ArrayList<>();

        PsiElementProcessor<PsiElement> processor = new PsiElementProcessor<PsiElement>() {
            @Override
            public boolean execute(@NotNull PsiElement each) {
                if (each == root) return true;
                if (each instanceof PasCaseStatement) {
                    caseElements.add((PasCaseStatement) each);
                } else if (each instanceof PasUsesClause) {
                    usesElements.add((PasUsesClause) each);
                } else if (each instanceof PasEnumType) {
                    enumElements.add((PasEnumType) each);
                } else if (each instanceof PasRoutineImplDeclImpl) {
                    routineElements.add((PasRoutineImplDeclImpl) each);
                } else if (each instanceof PsiComment) {
                    commentElements.add((PsiComment) each);
                } else if (PsiTreeUtil.instanceOf(each,
                        PasUnitInterface.class, PasUnitImplementation.class, PasUnitInitialization.class, PasUnitFinalization.class,
                        PasVarSection.class, PasTypeSection.class, PasConstSection.class,
                        PasClassTypeTypeDecl.class, PasClassHelperDecl.class, PasClassTypeDecl.class,
                        PasInterfaceTypeDecl.class, PasObjectDecl.class, PasRecordHelperDecl.class, PasRecordDecl.class,
                        PasCompoundStatement.class, PasHandler.class, PasRepeatStatement.class)) {
                    commonElements.add((PascalPsiElement) each);
                }
                return true;
            }
        };
        PsiTreeUtil.processElements(root, processor);

        foldCommon(descriptors, commonElements);
        foldCase(descriptors, caseElements);
        foldUses(descriptors, usesElements);
        foldEnums(descriptors, enumElements);
        foldRoutines(descriptors, routineElements);

        if (!quick) {
            foldComments(descriptors, document, commentElements);
        }

        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    private void foldRoutines(List<FoldingDescriptor> descriptors, Collection<PasRoutineImplDeclImpl> routineList) {
        for (PasRoutineImplDeclImpl routine : routineList) {
            int foldStart = getStartOffset(routine);
            TextRange range = getRange(foldStart, routine.getTextRange().getEndOffset());
            if (range.getLength() > 1) {
                descriptors.add(new NamedFoldingDescriptor(routine.getNode(), range, null,
                        " " + PsiUtil.normalizeRoutineName(routine) + ";"));
            }
        }
    }

    private void foldCommon(List<FoldingDescriptor> descriptors, Collection<PascalPsiElement> blocks) {
        for (final PsiElement block : blocks) {
            int foldStart = getStartOffset(block);
            TextRange range = getRange(foldStart, block.getTextRange().getEndOffset());
            if (range.getLength() > 1) {
                descriptors.add(new FoldingDescriptor(block.getNode(), range, null));
            }
        }
    }

    private int getStartOffset(PsiElement block) {
        return block.getFirstChild() != null ? block.getFirstChild().getTextRange().getEndOffset() : block.getTextRange().getStartOffset();
    }

    private TextRange getRange(int start, int end) {
        return new TextRange(start, end);
    }

    private void foldCase(List<FoldingDescriptor> descriptors, Collection<PasCaseStatement> caseStatements) {
        for (final PasCaseStatement caseStatement : caseStatements) {
            PsiElement caseItem = PsiUtil.getNextSibling(caseStatement.getFirstChild());
            if (caseItem != null) {
                caseItem = PsiUtil.getNextSibling(caseItem);
            }
            int foldStart = caseItem != null ? caseItem.getTextRange().getStartOffset() : caseStatement.getTextRange().getStartOffset();
            TextRange range = getRange(foldStart, caseStatement.getTextRange().getEndOffset());
            if (range.getLength() > 0) {
                descriptors.add(new FoldingDescriptor(caseStatement.getNode(), range, null));
            }
        }
    }

    private void foldUses(List<FoldingDescriptor> descriptors, Collection<PasUsesClause> usesList) {
        for (final PasUsesClause uses : usesList) {
            int foldStart = getStartOffset(uses);
            TextRange range = getRange(foldStart, uses.getTextRange().getEndOffset());
            if (range.getLength() > 1) {
                descriptors.add(new FoldingDescriptor(uses.getNode(), range, null) {
                    @Nullable
                    @Override
                    public String getPlaceholderText() {
                        StringBuilder sb = new StringBuilder(" ");
                        boolean first = true;
                        for (PascalQualifiedIdent ident : uses.getNamespaceIdentList()) {
                            if (!first) {
                                sb.append(", ").append(ident.getName());
                            } else {
                                sb.append(ident.getName());
                                first = false;
                            }
                        }
                        sb.append(";");
                        return sb.toString();
                    }
                });
            }
        }
    }

    private void foldEnums(List<FoldingDescriptor> descriptors, Collection<PasEnumType> enums) {
        for (final PasEnumType enumType : enums) {
            final PasTypeDeclaration decl = PsiTreeUtil.getParentOfType(enumType, PasTypeDeclaration.class);
            if (decl != null) {
                TextRange range = getRange(decl.getGenericTypeIdent().getTextRange().getEndOffset(), decl.getTextRange().getEndOffset());
                if (range.getLength() > 0) {
                    descriptors.add(new FoldingDescriptor(decl.getNode(), range, null) {
                        @Nullable
                        @Override
                        public String getPlaceholderText() {
                            StringBuilder sb = new StringBuilder(" = (");
                            boolean first = true;
                            for (PasNamedIdentDecl ident : enumType.getNamedIdentDeclList()) {
                                if (!first) {
                                    sb.append(", ").append(ident.getName());
                                } else {
                                    sb.append(ident.getName());
                                    first = false;
                                }
                            }
                            sb.append(");");
                            return sb.toString();
                        }
                    });
                }
            }
        }
    }

    private void foldComments(List<FoldingDescriptor> descriptors, Document document, final Collection<PsiComment> comments) {
        TextRange commentRange = null;
        PsiComment lastComment = null;
        for (final PsiComment comment : comments) {
            if ((null == lastComment) || (commentRange.getEndOffset() < comment.getTextRange().getStartOffset())) {
                lastComment = comment;
                final String endSymbol = getEndSymbol(lastComment);
                commentRange = comment.getTextRange();
                int commentEndLine = document.getLineNumber(commentRange.getEndOffset());
                // Merge sibling comments
                PsiElement sibling = PsiUtil.getNextSibling(comment);
                while (sibling instanceof PsiComment) {
                    TextRange nextRange = sibling.getTextRange();
                    if ((document.getLineNumber(nextRange.getStartOffset()) - commentEndLine) < 2) {
                        commentRange = commentRange.union(nextRange);
                    }
                    commentEndLine = document.getLineNumber(commentRange.getEndOffset());
                    sibling = PsiUtil.getNextSibling(sibling);
                }

                int lfPos = lastComment.getText().indexOf('\n') + lastComment.getTextOffset();
                if (lfPos < lastComment.getTextOffset()) {
                    lfPos = lastComment.getTextRange().getEndOffset();
                }
                if (lfPos < commentRange.getEndOffset()) {
                    descriptors.add(new FoldingDescriptor(lastComment.getNode(), getRange(lfPos, commentRange.getEndOffset()), null) {
                        @Nullable
                        @Override
                        public String getPlaceholderText() {
                            return "..." + endSymbol;
                        }
                    });
                }
            }
        }
    }

    private String getEndSymbol(PsiComment comment) {
        if (StringUtils.isNotEmpty(comment.getText())) {
            if (comment.getText().startsWith("{")) {
                return "}";
            } else if (comment.getText().startsWith("(*")) {
                return "*)";
            }
        }
        return "";
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return TOKENS_COLLAPSED.contains(node.getElementType());
    }
}