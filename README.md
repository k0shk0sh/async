# Async

A library designed to make the organized execution of async processes in Java easy.

---

# Table of Contents

1. [Gradle and Maven Dependency](https://github.com/afollestad/async#gradle-and-maven-dependency)
    1. [Gradle](https://github.com/afollestad/async#gradle)
    2. [Maven](https://github.com/afollestad/async#maven)
2. [Actions](https://github.com/afollestad/async#actions)
    1. [Basic Action](https://github.com/afollestad/async#basic-action)
    2. [Advanced Action](https://github.com/afollestad/async#advanced-action)
    3. [Action Execution](https://github.com/afollestad/async#action-execution)
3. [Pools](https://github.com/afollestad/async#pools)
    1. [Executing in Series](https://github.com/afollestad/async#executing-in-series)
    2. [Executing in Parallel](https://github.com/afollestad/async#executing-in-parallel)
    2. [Receiving Results](https://github.com/afollestad/async#receiving-results)
    3. [Pushing Actions](https://github.com/afollestad/async#pushing-actions)
5. [Cancellation](https://github.com/afollestad/async#cancellation)
    1. [Cancelling One](https://github.com/afollestad/async#cancelling-one)
    2. [Cancelling Multiple](https://github.com/afollestad/async#cancelling-multiple)
    3. [Cancelling All](https://github.com/afollestad/async#cancelling-all)

---

# Gradle and Maven Dependency

[![Release](https://jitpack.io/v/afollestad/async.svg)](https://jitpack.io/#afollestad/async)
[![Build Status](https://travis-ci.org/afollestad/async.svg)](https://travis-ci.org/afollestad/async)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

### Gradle

Add this to your project's root (not your module's) `build.gradle` file:

```Gradle
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

Add this to your module's (e.g. "app") `build.gradle` file (the version should match the 
one displayed on the badge above):

```Gradle
dependencies {
    ...
    compile 'com.github.afollestad:async:0.2.2'
}
```

### Maven

Add the repository:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependency (the version should match the one displayed on the badge above):

```xml
<dependency>
    <groupId>com.github.afollestad</groupId>
    <artifactId>async</artifactId>
    <version>x.y.z</version>
</dependency>
```

---

# Actions

### Basic Action

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

### Advanced Action

Here's a more advanced example:

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
```

Notice that `String` is passed as a generic parameter to `Action`. This specifies the return type for `run()` which, 
gets passed to `done(result)`.

### Action Execution

Executing an `Action` runs its code on a background thread.

```java
Action<Integer> action = // ...
action.execute();
```

You can receive the result in two ways, either through the optional `done()` method that can be 
overridden in your `Action`. Or you can wait for the result like this:

```java
Action<String> action = // ...
action.execute();
action.waitForExecution();
String result = action.getResult();
```

The downside to using `waitForExecution()` is that it will block the calling thread until the 
`Action` is done executing. When you override `done()` inside the `Action`, it will be called 
automatically when the background process is done.

---

# Pools

Async makes executing multiple actions in order **or** at the same time easy.

Take these two actions:

```java
Action one = // ...
Action two = // ...
```

### Executing in Series

Executing actions as a series means they will be executed one at a time.

```java
Async.series(one, two)
```

### Executing in Parallel

Executing actions in parallel means they will be executed at the same time.

```java
Async.parallel(one, two);
```

### Receiving Results

You can receive a callback when all actions are done. This is where the IDs of your actions
come in useful:

```java
Async.parallel(one, two);
    .done(new Done() {
        @Override
        public void result(@NonNull Result result) {
        
            // The parameter passed to get(String) must match an ID of an executed action
            Action<?> one = result.get("action-one-id");
            if (one != null) {
                // Do something with the result
                Object result1 = one.getResult();
            }
            
            Action<?> two = result.get("action-two-id");
            if (one != null) {
                // Do something with the result
                Object result2 = two.getResult();
            }
            
            // You can also iterate the Result object
            for (Action<?> action : result) {
                Object anotherResult = action.getResult();
            }
        }
    });
```

You can also receive results synchronously like you can with single actions, but remember that this 
blocks the calling thread:

```java
Action one = //...
Action two = //...

Pool pool = Async.series(one, two);
pool.waitForExecution();

Result result = pool.getResult();
// Use result object like shown in the callback for the async variant above
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

This would start by executing action `one` and `two` as a series (one at a time). `three` and 
`four` get pushed into the `Pool` after execution starts, and get executed in order after one 
and two are done.

In parallel mode, three and four would immediately get executed, but the `done` callback for all 
4 actions would get called at the same time as long as one and two didn't finish before three and 
four were pushed. If one and two finished before three and four were pushed, the done callback 
would be called a second time for three and four.

---

# Cancellation

### Cancelling One

Try to split up your action processing into multiple parts if possible. Between each part, 
check if the action has been cancelled yet using `isCancelled()`. Immediately returning null 
in that case will allow actions to stop executing as soon as possible when they are cancelled.

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

### Cancelling Multiple

```java
Action one = // ...
Action two = // ...

Pool pool = Async.parallel(one, two);
    .done(new Done() {
        @Override
        public void result(@NonNull Result result) {
            // Use the result, see the 'Receiving Results' section above
        }
    });
    
// Cancel all actions in the Pool
pool.cancel();
```

### Cancelling All

You can cancel all running pools:

```java
Async.cancelAll();
```

It's recommend you always call this every time your app goes into the background, to prevent 
memory leaks.
