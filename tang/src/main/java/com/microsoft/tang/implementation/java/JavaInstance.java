package com.microsoft.tang.implementation.java;

import com.microsoft.tang.implementation.InjectionPlan;
import com.microsoft.tang.types.Node;

final public class JavaInstance<T> extends InjectionPlan<T> {
    final T instance;

    public JavaInstance(Node name, T instance) {
      super(name);
      this.instance = instance;
    }

    @Override
    public int getNumAlternatives() {
      return instance == null ? 0 : 1;
    }

    @Override
    public String toString() {
      return getNode() + " = " + instance;
    }

    @Override
    public boolean isAmbiguous() {
      return false;
    }

    @Override
    public boolean isInjectable() {
      return instance != null;
    }

    public String getInstanceAsString() {
      return instance.toString();
    }
    
    @Override
    protected String toAmbiguousInjectString() {
      throw new IllegalArgumentException("toAmbiguousInjectString called on JavaInstance!" + this.toString());
    }

    @Override
    protected String toInfeasibleInjectString() {
      return getNode() + " is not bound.";
    }

    @Override
    protected boolean isInfeasibleLeaf() {
      return true;
    }

    @Override
    public String toShallowString() {
      return toString();
    }

    @Override
    public boolean hasFutureDependency() {
      return false;
    }
  }