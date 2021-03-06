package com.ericlam.mc.eldgui.event;

import com.ericlam.mc.eldgui.controller.UIController;
import com.ericlam.mc.eldgui.lifecycle.OnDestroy;
import com.ericlam.mc.eldgui.lifecycle.OnRendered;
import org.apache.commons.lang.Validate;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class LifeCycleManager {

    private final UIController controller;
    private final MethodParseManager methodParseManager;

    private final Map<Class<? extends Annotation>, Method> lifeCycleMap = new ConcurrentHashMap<>();

    public LifeCycleManager(UIController controller, MethodParseManager methodParseManager) {
        this.controller = controller;
        this.methodParseManager = methodParseManager;
        this.loadLifeCycle(OnRendered.class);
        this.loadLifeCycle(OnDestroy.class);
    }


    private <A extends Annotation> void loadLifeCycle(Class<A> lifeCycle) {
        List<Method> methods = Arrays.stream(controller.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(lifeCycle)).collect(Collectors.toList());
        if (methods.size() > 1)
            throw new IllegalStateException(lifeCycle.getName()+" lifecycle must at most once in each controller.");
        if (!methods.isEmpty()) {
            this.lifeCycleMap.put(lifeCycle, verifyReturnType(methods.get(0)));
        }
    }

    public <A extends Annotation> void onLifeCycle(Class<A> lifeCycle){
        Optional.ofNullable(this.lifeCycleMap.get(lifeCycle)).ifPresent(m -> {
            try {
                m.invoke(controller, methodParseManager.getMethodParameters(m));
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }

    private Method verifyReturnType(Method m){
        Validate.isTrue(m.getReturnType().equals(void.class), "return type of the LifeCycle method must be void");
        return m;
    }

}
