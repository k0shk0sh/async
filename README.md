# Async

A library designed to make the organized execution of async processes on Android easy.

---

# Gradle Dependency

[![Release](https://img.shields.io/github/release/afollestad/async.svg?label=jitpack)](https://jitpack.io/#afollestad/async)

### Repository

Add this to your project's root (not your module's) `build.gradle` file:

```Gradle
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

### Dependency

Add this to your module's (e.g. "app") `build.gradle` file:

```Gradle
dependencies {
    ...
    compile 'com.github.afollestad:async:0.1.0'
}
```

---

# Table of Contents

1. [Gradle Dependency](https://github.com/afollestad/async#gradle-dependency)
    1. [Repository](https://github.com/afollestad/async#repository)
    2. [Dependency](https://github.com/afollestad/async#dependency)
2. [Actions](https://github.com/afollestad/async#actions)
3. [Execution](https://github.com/afollestad/async#execution)
    1. [Singular](https://github.com/afollestad/async#singular)
    2. [Multiple](https://github.com/afollestad/async#multiple)
    3. [Pushing Actions](https://github.com/afollestad/async#pushing-actions)
4. [Cancellation](https://github.com/afollestad/async#cancellation)
    1. [Singular](https://github.com/afollestad/async#singular-1)
    2. [Multiple](https://github.com/afollestad/async#multiple-1)
    3. [All](https://github.com/afollestad/async#all)

---

# Actions

An action is a single process. At a bare minimum, they look like this:

```java
Action action = new Action() {
    @NonNull
    @Override
    public String id() {
        // Return a unique ID.
        return "my-action";
    }

    @Nullable
    @Override
    protected Object run() throws InterruptedException {
        // Do background processing, return a result if any.
        return null;
    }
};
```

The value returned in `id()` should be unique for all actions in your app. It'll be useful later.

---

# Execution

### Singular

Executing a single action is easy:

```java
Action<String> action = new Action<String>() {
    @NonNull
    @Override
    public String id() {
        // Return a unique ID.
        return "my-action";
    }

    @Nullable
    @Override
    protected String run() throws InterruptedException {
        // Do something in the background, return the result.
        return null;
    }

    @Override
    protected void done(@Nullable String result) {
        // Back on the UI thread. Use the result.
    }
};

action.execute();
```

The `run()` method is called on a background thread, the `done(result)` method is called back on the UI thread. 
  **When an action is executed, it can't be executed again until it's done.**

### Multiple

Async makes executing multiple actions in order or at the same time easy.

Take these two actions:

```java
Action<String> one = new Action<String>() {
    @NonNull
    @Override
    public String id() {
        return "one";
    }

    @Nullable
    @Override
    protected String run() throws InterruptedException {
        return "Hello, ";
    }
};
Action<String> two = new Action<String>() {
    @NonNull
    @Override
    public String id() {
        return "two";
    }

    @Nullable
    @Override
    protected String run() throws InterruptedException {
        return "how are you?";
    }
};
```

You can execute them as a series (one at a time):

```java
Async.series(one, two)
```

Or parallel to each other (at the same time):

```java
Async.parallel(one, two);
```

You can even receive a callback when all actions are done. This is where the IDs of your actions
come in useful:

```java
Async.parallel(one, two);
    .done(new Done() {
        @Override
        public void result(@NonNull Result result) {
            // All actions are done executing, use the results.
            String resultOne = (String) result.get("one"); // "one" is an id() value
            String resultTwo = (String) result.get("two"); // "two" is an id() value
        }
    });
```

### Pushing Actions

Pools allow you to continue to push actions to be executed.

For an example:

```java
Action one = //...
Action two = //...
Action three = //...
Action four = //...

Pool pool = Async.series(one, two);

pool.push(three, four);
```

This would start by executing action one and two as a series (one at a time). Three and four get pushed
into the Pool after execution starts, and get executed in order after one and two are done.

In parallel mode, three and four would immediately get executed, but the `done` callback for all 4 actions
would get called at the same time as long as one and two didn't finish before three and four were pushed.
If one and two finished before three and four were pushed, the done callback would be called a second time for
three and four.

---

# Cancellation

### Singular

Try to split up your action processing into multiple parts if possible. Between each part, check if the
 action has been cancelled yet using `isCancelled()`. Immediately returning null in that case will allow
 actions to stop executing as soon as possible when they are cancelled.

```java
Action<String> action = new Action<String>() {
    @NonNull
    @Override
    public String id() {
        // Return a unique ID.
        return "my-action";
    }

    @Nullable
    @Override
    protected String run() throws InterruptedException {
        // Begin processing
        if (isCancelled())
            return null;
        // Not cancelled yet, finish processing
        return null;
    }

    @Override
    protected void done(@Nullable String result) {
        // Back on the UI thread, action was NOT cancelled. Use the result.
    }
};

action.execute();
action.cancel();
```

### Multiple

```java
Action<String> one = new Action<String>() {
    @NonNull
    @Override
    public String id() {
        return "one";
    }

    @Nullable
    @Override
    protected String run() throws InterruptedException {
        return "Hello, ";
    }
};
Action<String> two = new Action<String>() {
    @NonNull
    @Override
    public String id() {
        return "two";
    }

    @Nullable
    @Override
    protected String run() throws InterruptedException {
        return "how are you?";
    }
};

Pool pool = Async.parallel(one, two);
    .done(new Done() {
        @Override
        public void result(@NonNull Result result) {
            // All actions are done executing, they were NOT cancelled. Use the results.
            String resultOne = (String) result.get("one"); // "one" is an id() value
            String resultTwo = (String) result.get("two"); // "two" is an id() value
        }
    });
    
// Cancel all actions in the Pool
pool.cancel();
```

### All

You can cancel all running pools:

```java
Async.cancelAll();
```

It's recommend you always call this every time your app goes into the background, to prevent memory leaks.
