package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigTemplateGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if (TwigHelper.getGoToBlockPattern().accepts(psiElement)) {
            return this.getBlockGoTo(psiElement);
        }

        if (TwigHelper.getAutocompletableTemplatePattern().accepts(psiElement)) {
            return this.getTwigFiles(psiElement);
        }

        // find trans('', {}, '|')
        // tricky way to get the function string trans(...)
        if (TwigHelper.getTransDomainPattern().accepts(psiElement)) {
            PsiElement psiElementTrans = PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("trans"));
            if(psiElementTrans != null && TwigHelper.getTwigMethodString(psiElementTrans) != null) {
                return TranslationUtil.getDomainFilePsiElements(psiElement.getProject(), psiElement.getText());
            }
        }

        return null;
    }

    private PsiElement[] getTwigFiles(PsiElement psiElement) {
        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(psiElement.getProject());
        TwigFile twigFile = twigFilesByName.get(psiElement.getText());

        if (null == twigFile) {
            return null;
        }

        return new PsiElement[] { twigFile };
    }

    private PsiElement[] getBlockGoTo(PsiElement psiElement) {
        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(psiElement.getProject());
        ArrayList<TwigBlock> blocks = new TwigBlockParser(twigFilesByName).walk(psiElement.getContainingFile());

        ArrayList<PsiElement> psiElements = new ArrayList<PsiElement>();
        for (TwigBlock block : blocks) {
            if(block.getName().equals(psiElement.getText())) {
                Collections.addAll(psiElements, block.getBlock());
            }
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
