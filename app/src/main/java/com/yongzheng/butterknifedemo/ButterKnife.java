package com.yongzheng.butterknifedemo;

import android.app.Activity;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;
import com.example.Unbinder;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简单ButterKnife实现
 * Created by yongzheng on 17-8-5.
 */
public class ButterKnife {

    private static final String TAG = "ButterKnife";
    private static boolean debug = false;

    //用于缓存,以后使用不用再次放射
    @VisibleForTesting
    static final Map<Class<?>, Constructor<? extends Unbinder>> BINDINGS = new LinkedHashMap<>();

    @NonNull
    @UiThread
    public static Unbinder bind(@NonNull Activity target) {
        View sourceView = target.getWindow().getDecorView();
        return createBinding(target, sourceView);
    }

    private static Unbinder createBinding(Activity target, View source) {
        Class<?> targetClass = target.getClass();
        if (debug) Log.d(TAG, "Looking up binding for " + targetClass.getName());
        Constructor<? extends Unbinder> constructor = findBindingConstructorForClass(targetClass);

        if (constructor == null) {
            return Unbinder.EMPTY;
        }
        //noinspection TryWithIdenticalCatches Resolves to API 19+ only type.
        try {
            //创建一个viewbing
            return constructor.newInstance(target, source);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Unable to invoke " + constructor, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Unable to create binding instance.", cause);
        }
    }

    /**
     * 查找构造函数,如果当前类找不到,就找父类的,一层层遍历上去
     * 直到包名是android或者java开头的结束
     *
     * 开始递归父类 findBindingConstructorForClass(cls.getSuperclass())
     * 停止递归    if (clsName.startsWith("android.") || clsName.startsWith("java."))
     * @param cls
     * @return
     */
    @Nullable
    @CheckResult
    @UiThread
    private static Constructor<? extends Unbinder> findBindingConstructorForClass(Class<?> cls) {
        //先在缓存中获取
        Constructor<? extends Unbinder> bindingCtor = BINDINGS.get(cls);
        if (bindingCtor != null) {
            if (debug) Log.d(TAG, "HIT: Cached in binding map.");
            return bindingCtor;
        }
        //去除包名是android和java的,官方的ButterKnife业务,我这里直接拷贝过来
        //这里的android.,下面由于一直要遍历父类,直到包名为android开头的返回
        String clsName = cls.getName();
        if (clsName.startsWith("android.") || clsName.startsWith("java.")) {
            if (debug) Log.d(TAG, "MISS: Reached framework class. Abandoning search.");
            return null;
        }
        try {
            //放射查找viewbinding的实现类
            Class<?> bindingClass = Class.forName(clsName + "_ViewBinding");
            //获取构造函数(Activity,view)
            bindingCtor = (Constructor<? extends Unbinder>) bindingClass.getConstructor(cls, View.class);
            if (debug) Log.d(TAG, "HIT: Loaded binding class and constructor.");
        } catch (ClassNotFoundException e) {
            //如果找不到,就找父类的,直到找到包名是android开头的
            if (debug) Log.d(TAG, "Not found. Trying superclass " + cls.getSuperclass().getName());
            bindingCtor = findBindingConstructorForClass(cls.getSuperclass());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to find binding constructor for " + clsName, e);
        }
        //如果找到了,放入缓存中,下次就不用再次查找,直接使用就可以了
        BINDINGS.put(cls, bindingCtor);
        return bindingCtor;
    }

}
