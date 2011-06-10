package jenkins.repository.updateCenter;

import hudson.util.PluginServletFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;

public class UpdateCenterFilter extends PluginServletFilter {

    private static final Pattern REPOSITORY_UPDATECENTER_URL = Pattern.compile("^.*plugin/repository/updates/update-center\\.json$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            if (isRepositoryUpdateCenterUrl(((HttpServletRequest) request).getPathInfo())) {
                merge(request, response, chain);
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void merge(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        MyHttpServletResponseWrapper wrapper = new MyHttpServletResponseWrapper((HttpServletResponse) response);
        chain.doFilter(request, wrapper);

        JSONObject local = toJSON(new String(wrapper.stream.baos.toByteArray(), "UTF-8"));
        JSONObject remote = getRemoteJSON();
        JSONObject plugins = remote.getJSONObject("plugins");
        plugins.putAll(local.getJSONObject("plugins"));

        response.getWriter().append(String.format("updateCenter.post(%n%s%n);", remote));
    }

    private JSONObject getRemoteJSON() {
        InputStream is = null;
        try {
            is = new URL("http://updates.jenkins-ci.org/update-center.json").openStream();
            return toJSON(IOUtils.toString(is));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(is);
        }
        return null;
    }

    private JSONObject toJSON(String jsonp) {
        return JSONObject.fromObject(jsonp.substring(jsonp.indexOf('(') + 1, jsonp.lastIndexOf(')')));
    }

    @Override
    public void destroy() {}

    private boolean isRepositoryUpdateCenterUrl(String pathInfo) {
        return REPOSITORY_UPDATECENTER_URL.matcher(pathInfo).matches();
    }

    public static class MyServletOutputStream extends ServletOutputStream {

        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        @Override
        public void write(int b) throws IOException {
            baos.write(b);
        }
    }

    public static class MyHttpServletResponseWrapper extends HttpServletResponseWrapper {

        private MyServletOutputStream stream;

        public MyHttpServletResponseWrapper(HttpServletResponse response) {
            super(response);
            stream = new MyServletOutputStream();
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return stream;
        }
    }
}
