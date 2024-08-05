package net.my.interceptor;

import net.my.pojo.UserBase;
import net.my.util.SpringContextUtil;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        System.out.println("----------supportsParameter-----------" + parameter.getParameterType());
        return parameter.getParameterType().isAssignableFrom(UserBase.class)//判断是否能转成UserBase 类型
                && parameter.hasParameterAnnotation(CurrentUser.class);//是否有CurrentUser注解
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        System.out.println("--------------resolveArgument-------------" + parameter);
        UserBase user = (UserBase) webRequest.getAttribute(CurrentUserConstants.CURRENT_USER, RequestAttributes.SCOPE_REQUEST);
        if (user != null) {
            return user;
        } else {
            if(!SpringContextUtil.enableJwtFilter()) {
                return UserBase.builder().userId("nobody").name("无名氏").build();
            }
        }
        throw new MissingServletRequestPartException(CurrentUserConstants.CURRENT_USER);
    }
}