# Introduction #

Although support for complex numbers is not built-in, the expression parser is build for you to easily add it in with minimal coding.

# Adding support to parse complex literals #

You can do that through two manners:
  * Add a complex function, to allow jmep syntax as `3i`
  * Add a Unit of Measure for the imaginary part, to allow jmep syntax as `complex(1,2)`

As exemplified by this code:
```
env.addFunction("complex",(Object [] a)->new Complex(a[0],a[1]));
env.registerUnit("i",Double.class,(i)->new Complex(0,i));
```

Now you can at least generate complex numbers in the jmep syntax:
  * `3i`
  * `complex(1,2)`

Now you won't still be able to do any real calculating on it ... there are still no paramours defined.

# Adding Operator support #

Lets add first support for the + operator, so we at least can write something like `1+2i` instead of `complex(1,2)`, so we have a little more natural way of writing complex numbers. Doing this is pretty simple.
First make sure you have the right imports added:
```
import static com.googlecode.jmep.BinaryOperatorType.*;
import static com.googlecode.jmep.UnaryOperatorType.*;
```

Then you can simply register the operators:
```
env.register(ADD,Complex.class,Complex.class,(l,r)->l.add(r));
```

The assumption for coding it like this is of course that the Complex libraries you are using are written in a way that Complex is actually an immutable object, or at least that the add operator returns a new Complex object and does not directly operate directly on either sides of the operands.

This will still be not enough, for this to work, we need to define up-conversion. This is needed to make sure we can operate on things like Long and or Double directly, otherwise it would only work if left and Right would be both Complex.

```
env.register(Long.class,Complex.class,(r)->new Complex(r,0));
env.register(Double.class,Complex.class,(r)->new Complex(r,0));
```