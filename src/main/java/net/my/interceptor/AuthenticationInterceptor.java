package net.my.interceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import net.my.exception.CommonException;
import net.my.pojo.UserBase;
import net.my.service.IUserBaseService;
import net.my.util.SpringContextUtil;
import net.my.util.TokenUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Slf4j
public class AuthenticationInterceptor implements HandlerInterceptor {
    @Resource
    IUserBaseService userBaseService;

    // 在业务处理器处理请求之前被调用
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // log.info("====> jwt.filter={}", SpringContextUtil.getProperty("jwt.filter", Boolean.class));
        if(!SpringContextUtil.enableJwtFilter()) {
            return true;
        }

        // 如果不是映射到方法直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();
        // 判断接口是否需要登录
        LoginRequired methodAnnotation = method.getAnnotation(LoginRequired.class);

        // 有 @LoginRequired 注解，需要认证
        if (methodAnnotation != null) {
            // 判断是否存在令牌信息，如果存在，则允许登录
            String accessToken = request.getHeader("Authorization");

            if (null == accessToken) {
                throw new CommonException(401, "无token，请重新登录");
            } else {
                // 从Redis 中查看 token 是否过期
                Claims claims;
                try{
                    claims = TokenUtils.parseJWT(accessToken);
                }catch (ExpiredJwtException e){
                    response.setStatus(401);
                    throw new CommonException(401, "token失效，请重新登录");
                }catch (Exception se){
                    response.setStatus(401);
                    throw new CommonException(401, "token令牌错误");
                }

                String userName = claims.getId();
                UserBase user = userBaseService.findUserByAccount(userName);
                if (user == null) {
                    response.setStatus(401);
                    throw new CommonException(401, "用户不存在，请重新登录");
                }
                // 当前登录用户@CurrentUser
                request.setAttribute(CurrentUserConstants.CURRENT_USER, user);
                return true;
            }

        } else {//不需要登录可请求
            return true;
        }
    }
    // 请求处理之后进行调用，但是在视图被渲染之前（Controller方法调用之后）
    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    // 在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）
    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
    }
}