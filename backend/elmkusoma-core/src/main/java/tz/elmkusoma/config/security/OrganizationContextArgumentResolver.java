package tz.elmkusoma.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class OrganizationContextArgumentResolver implements HandlerMethodArgumentResolver {

    private final OrganizationContextHolder contextHolder;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return OrganizationContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) {
        OrganizationContext context = contextHolder.getContext();
        if (context == null) {
            throw new IllegalStateException("OrganizationContext not available. Ensure OrganizationContextResolver filter is applied.");
        }
        return context;
    }
}