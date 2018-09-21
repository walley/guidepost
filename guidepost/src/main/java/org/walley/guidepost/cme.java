/* 
Copyright 2013-2016 Michal Grezl

This file is part of Guidepost.

Guidepost is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Guidepost is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Guidepost.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.walley.guidepost;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;

public class cme extends MultipartEntity
{

  private final ProgressListener listener;

  public cme(final ProgressListener listener)
  {
    super();
    this.listener = listener;
  }

  public cme(final HttpMultipartMode mode, final ProgressListener listener)
  {
    super(mode);
    this.listener = listener;
  }

  public cme(HttpMultipartMode mode, final String boundary, final Charset charset, final ProgressListener listener)
  {
    super(mode, boundary, charset);
    this.listener = listener;
  }

  @Override
  public void writeTo(final OutputStream outstream) throws IOException
  {
    super.writeTo(new count_o_s(outstream, this.listener));
  }

  public static interface ProgressListener
  {
    void transferred(long num);
  }

  public static class count_o_s extends FilterOutputStream
  {

    private final ProgressListener listener;
    private long transferred;

    public count_o_s(final OutputStream out, final ProgressListener listener)
    {
      super(out);
      this.listener = listener;
      this.transferred = 0;
    }

    public void write(byte[] b, int off, int len) throws IOException
    {
      out.write(b, off, len);
      this.transferred += len;
      this.listener.transferred(this.transferred);
    }

    public void write(int b) throws IOException
    {
      out.write(b);
      this.transferred++;
      this.listener.transferred(this.transferred);
    }
  }
}
