/*
 * Copyright (c) 2009 Matthew Hildebrand <matt.hildebrand@gmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package org.fusesource.scalate.servlet

import java.io.File
import javax.servlet.ServletContext
import org.fusesource.scalate.ResourceNotFoundException
import org.fusesource.scalate.support.Resource._
import java.lang.String
import org.fusesource.scalate.support.{ResourceLoader, FileResourceLoader}

object ServletResourceLoader {
  def apply(context: ServletContext) = new ServletResourceLoader(context)
}
/**
 * Loads files using <code>ServletContext</code>.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ServletResourceLoader(context: ServletContext, delegate: ResourceLoader = new FileResourceLoader()) extends ResourceLoader {

  override def resource(uri: String) = {
    val file = realFile(uri)
    if (file != null) {
      Some(fromFile(file))
    }
    else {
      val url = context.getResource(uri)
      if (url != null) {
        Some(fromURL(url))
      }
      else {
        delegate.resource(uri)
      }
    }
  }

  /**
   * Returns the real path for the given uri.
   */
  def realPath(uri: String): String = {
    // TODO should ideally use the Resource API as then we could try figure out
    // the actual file for URL based resources not using getRealPath
    // (which has issues sometimes with unexpanded WARs and overlays)
    for (r <- resource(uri); f <- r.toFile if f != null) {
      return f.getPath
    }
    val file = realFile(uri)
    if (file != null) file.getPath else null
  }


  override protected def createNotFoundException(uri: String) =  new ResourceNotFoundException(resource = uri, root = context.getRealPath("/"))

  /**
   * Returns the File for the given uri
   */
  protected def realFile(uri: String): File = {
    def findFile(uri: String): File = {
      val path = context.getRealPath(uri)
      debug("realPath for: " + uri + " is: " + path)

      var answer: File = null
      if (path != null) {
        val file = new File(path)
        debug("file from realPath for: " + uri + " is: " + file)
        if (file.canRead) { answer = file}
      }
      answer
    }

    findFile(uri) match {
      case file: File => file
      case _ => if (uri.startsWith("/") && !uri.startsWith("/WEB-INF")) {
        findFile("/WEB-INF" + uri)  
      }
      else {
        null
      }
    }
  }

}
