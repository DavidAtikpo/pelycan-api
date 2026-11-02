package com.pelycan.api.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/** Firebase bean only when {@code pelycan.firebase.credentials-path} is non-blank. */
final class FirebaseCredentialsPresentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String path = context.getEnvironment().getProperty("pelycan.firebase.credentials-path");
        return StringUtils.hasText(path);
    }
}
