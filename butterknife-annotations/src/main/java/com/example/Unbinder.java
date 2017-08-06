package com.example;

/** An unbinder contract that will unbind views when called. */
public interface Unbinder {

  /**
   * 取消绑定
   */
  void unbind();

  /**
   * 一个空实现的unbinder
   * findBindingConstructorForClass 找不到时候,就返回这个
   */
  Unbinder EMPTY = new Unbinder() {
    @Override public void unbind() { }
  };

}
