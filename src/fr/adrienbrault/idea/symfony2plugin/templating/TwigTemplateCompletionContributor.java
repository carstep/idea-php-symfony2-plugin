package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationIndex;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslatorLookupElement;
import fr.adrienbrault.idea.symfony2plugin.translation.parser.TranslationStringMap;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigTemplateCompletionContributor extends CompletionContributor {

    public TwigTemplateCompletionContributor() {
        extend(
            CompletionType.BASIC,
            TwigHelper.getAutocompletableTemplatePattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    if(!TwigHelper.isTemplateFileReferenceTag(parameters.getPosition())) {
                        return;
                    }

                    Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(parameters.getPosition().getProject());
                    for (Map.Entry<String, TwigFile> entry : twigFilesByName.entrySet()) {
                        resultSet.addElement(
                            new TemplateLookupElement(entry.getKey(), entry.getValue())
                        );
                    }
                }
            }
        );

        // provides support for 'a<xxx>'|trans({'%foo%' : bar|default}, 'Domain')
        extend(
            CompletionType.BASIC,
            TwigHelper.getTranslationPattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    TranslationStringMap map = TranslationIndex.getInstance(parameters.getPosition().getProject()).getTranslationMap();
                    if(map == null) {
                        return;
                    }

                    PsiElement psiElement = parameters.getPosition();
                    String domainName =  YamlHelper.getDomainTrans(psiElement);

                    ArrayList<String> domainMap = map.getDomainMap(domainName);
                    if(domainMap == null) {
                        return;
                    }

                    for(String stringId : domainMap) {
                        resultSet.addElement(
                            new TranslatorLookupElement(stringId, domainName)
                        );
                    }

                }
            }

        );

        // provides support for 'a'|trans({'%foo%' : bar|default}, '<xxx>')
        extend(
            CompletionType.BASIC,
            TwigHelper.getTransDomainPattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    if(PsiElementUtils.getPrevSiblingOfType(parameters.getPosition(), PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("trans")) == null) {
                        return;
                    }

                    TranslationStringMap map = TranslationIndex.getInstance(parameters.getPosition().getProject()).getTranslationMap();
                    if(map != null) {
                        for(String domainKey : map.getDomainList()) {
                            resultSet.addElement(new TranslatorLookupElement(domainKey, domainKey));
                        }
                    }

                }


            }

        );

        // provides support for {% block |
        extend(
            CompletionType.BASIC,
            TwigHelper.getBlockTagPattern(),
            new CompletionProvider<CompletionParameters>() {
                public void addCompletions(@NotNull CompletionParameters parameters,
                                           ProcessingContext context,
                                           @NotNull CompletionResultSet resultSet) {

                    if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
                        return;
                    }

                    Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(parameters.getPosition().getProject());
                    ArrayList<TwigBlock> blocks = new TwigBlockParser(twigFilesByName).walk(parameters.getPosition().getContainingFile());
                    ArrayList<String> uniqueList = new ArrayList<String>();
                    for (TwigBlock block : blocks) {
                        if(!uniqueList.contains(block.getName())) {
                            uniqueList.add(block.getName());
                            resultSet.addElement(new TwigBlockLookupElement(block));
                        }
                    }

                }
            }
        );

    }

}
