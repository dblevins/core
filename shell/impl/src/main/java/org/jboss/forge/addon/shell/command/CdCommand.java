/**
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.shell.command;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.util.ResourcePathResolver;
import org.jboss.forge.addon.shell.spi.command.CdTokenHandler;
import org.jboss.forge.addon.shell.ui.AbstractShellCommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.furnace.services.Imported;

/**
 * Changes to the current directory
 * 
 * @author <a href="ggastald@redhat.com">George Gastaldi</a>
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class CdCommand extends AbstractShellCommand
{
   private static final Logger log = Logger.getLogger(CdCommand.class.getName());

   @Inject
   private ResourceFactory resourceFactory;

   @Inject
   private Imported<CdTokenHandler> handlers;

   @Inject
   @WithAttributes(label = "Arguments", type = InputType.FILE_PICKER)
   private UIInputMany<String> arguments;

   @Override
   public void initializeUI(UIBuilder builder) throws Exception
   {
      builder.add(arguments);
   }

   @Override
   public UICommandMetadata getMetadata(UIContext context)
   {
      return Metadata.from(super.getMetadata(context), getClass()).name("cd")
               .description("Change the current directory");
   }

   @Override
   public Result execute(UIExecutionContext context) throws Exception
   {
      UIContext uiContext = context.getUIContext();
      Iterable<String> value = arguments.getValue();
      Iterator<String> it = value == null ? Collections.<String> emptyIterator() : value.iterator();
      final Result result;
      if (it.hasNext())
      {
         String token = it.next();

         List<Resource<?>> newResource = null;
         for (CdTokenHandler handler : handlers)
         {
            try
            {
               newResource = handler.getNewCurrentResources(uiContext, token);

               if (newResource != null && newResource.isEmpty())
                  newResource = null;
               else
                  break;
            }
            catch (Exception e)
            {
               log.log(Level.WARNING, "Error encountered during processing of [" + handler + "] for path token ["
                        + token + "].", e);
            }
            finally
            {
               handlers.release(handler);
            }
         }

         if (newResource == null)
         {
            Resource<?> currentResource = (Resource<?>) uiContext.getInitialSelection().get();
            newResource = new ResourcePathResolver(resourceFactory, currentResource, token).resolve();
         }

         if (newResource.isEmpty() || !newResource.get(0).exists())
         {
            result = Results.fail(token + ": No such file or directory");
         }
         else
         {
            FileResource<?> newFileResource = newResource.get(0).reify(FileResource.class);
            if (newFileResource == null)
            {
               result = Results.fail(token + ": Invalid path");
            }
            else
            {
               uiContext.setSelection(newFileResource);
               result = Results.success();
            }
         }
      }
      else
      {
         result = Results.success();
      }
      return result;
   }
}
