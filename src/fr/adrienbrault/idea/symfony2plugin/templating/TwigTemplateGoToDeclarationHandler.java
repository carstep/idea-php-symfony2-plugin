package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigTemplateGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !PlatformPatterns.psiElement().withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {
            return null;
        }

        if (TwigHelper.getBlockTagPattern().accepts(psiElement)) {
            return this.getBlockGoTo(psiElement);
        }

        if (TwigHelper.getPathAfterLeafPattern().accepts(psiElement)) {
            PsiElement[] psiElements = this.getRouteParameterGoTo(psiElement);
            if(psiElements.length > 0) {
                return psiElements;
            }
        }

        // support: {% include() %}, {{ include() }}
        if(TwigHelper.getTemplateFileReferenceTagPattern().accepts(psiElement) || TwigHelper.getPrintBlockFunctionPattern("include").accepts(psiElement)) {
            return this.getTwigFiles(psiElement);
        }

        if(TwigHelper.getAutocompletableRoutePattern().accepts(psiElement)) {
            return this.getRouteGoTo(psiElement);
        }

        // find trans('', {}, '|')
        // tricky way to get the function string trans(...)
        if (TwigHelper.getTransDomainPattern().accepts(psiElement)) {
            PsiElement psiElementTrans = PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")));
            if(psiElementTrans != null && TwigHelper.getTwigMethodString(psiElementTrans) != null) {
                return TranslationUtil.getDomainFilePsiElements(psiElement.getProject(), psiElement.getText());
            }
        }

        if (TwigHelper.getTranslationPattern("trans", "transchoice").accepts(psiElement)) {
            return getTranslationKeyGoTo(psiElement);
        }

        // provide global twig file resolving
        if (PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .withText(PlatformPatterns.string().endsWith(".twig")).accepts(psiElement)) {

            return this.getTwigFiles(psiElement);
        }

        if(TwigHelper.getPrintBlockFunctionPattern("controller").accepts(psiElement)) {
            PsiElement controllerMethod = this.getControllerGoTo(psiElement);
            if(controllerMethod != null) {
                return new PsiElement[] { controllerMethod };
            }
        }

        if(TwigHelper.getTransDefaultDomain().accepts(psiElement)) {
            return TranslationUtil.getDomainFilePsiElements(psiElement.getProject(), psiElement.getText());
        }

        if(TwigHelper.getAutocompletableFilterPattern().accepts(psiElement)) {
            return getFilterGoTo(psiElement);
        }

        return null;
    }

    private PsiElement[] getRouteParameterGoTo(PsiElement psiElement) {

        String routeName = TwigHelper.getMatchingRouteNameOnParameter(psiElement);
        if(routeName == null) {
            return new PsiElement[0];
        }

        return RouteHelper.getRouteParameterPsiElements(psiElement.getProject(), routeName, psiElement.getText());
    }

    private PsiElement getControllerGoTo(PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        return ControllerIndex.getControllerMethod(psiElement.getProject(), text);
    }

    @Nullable
    private PsiElement[] getTwigFiles(PsiElement psiElement) {

        String templateName = psiElement.getText();
        PsiElement[] psiElements = TwigHelper.getTemplatePsiElements(psiElement.getProject(), templateName);

        if(psiElements.length == 0) {
            return null;
        }

        return psiElements;
    }

    private PsiElement[] getFilterGoTo(PsiElement psiElement) {
        HashMap<String, TwigExtension> filters = new TwigExtensionParser(psiElement.getProject()).getFilters();
        if(!filters.containsKey(psiElement.getText())) {
            return new PsiElement[0];
        }

        String signature = filters.get(psiElement.getText()).getSignature();
        if(signature == null) {
            return new PsiElement[0];
        }

        return PhpElementsUtil.getPsiElementsBySignature(psiElement.getProject(), signature);
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

    private PsiElement[] getRouteGoTo(PsiElement psiElement) {
        return RouteHelper.getMethods(psiElement.getProject(), PsiElementUtils.getText(psiElement));
    }

    private PsiElement[] getTranslationKeyGoTo(PsiElement psiElement) {
        String translationKey = psiElement.getText();
        return TranslationUtil.getTranslationPsiElements(psiElement.getProject(), translationKey, TwigUtil.getPsiElementTranslationDomain(psiElement));
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
