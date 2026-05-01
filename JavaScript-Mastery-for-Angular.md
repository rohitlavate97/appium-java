# JavaScript Mastery for Angular

> **Goal:** Achieve master-level JavaScript knowledge specifically engineered for building enterprise Angular applications.  
> **Approach:** Senior engineer mentorship — concise, deep, practical, progressive.

---

# Module 1 – JavaScript Fundamentals

---

## 1.1 What is JavaScript?

### Simple Explanation
JavaScript is a lightweight, interpreted, single-threaded programming language that runs in browsers and on servers (Node.js). It is the language of the web.

### Deep Explanation
JavaScript is:
- **Dynamically typed** — variable types are resolved at runtime, not compile time.
- **Interpreted / JIT-compiled** — modern engines (V8, SpiderMonkey) compile JS to machine code on the fly.
- **Single-threaded** — one call stack, one thing at a time. Concurrency is achieved via the Event Loop (covered in Module 6).
- **Prototype-based OOP** — inheritance via prototype chains, not classical classes (ES6 `class` is syntactic sugar).
- **Multi-paradigm** — supports imperative, OOP, and functional styles.

### Mental Model
> Think of JavaScript as a *chef working alone in a kitchen*. One task at a time, but smart enough to delegate waiting tasks (async) to helpers and return to them when ready.

### Angular Relevance
Angular is written in TypeScript, which compiles to JavaScript. Every concept here is the engine under Angular's hood — components, services, pipes, RxJS operators all depend on these fundamentals.

---

## 1.2 Variables — `var`, `let`, `const`

### Simple Explanation
Variables store data. Use `const` by default, `let` when reassignment is needed, never `var`.

### Deep Explanation

| Feature        | `var`              | `let`              | `const`            |
|----------------|--------------------|--------------------|--------------------|
| Scope          | Function-scoped    | Block-scoped       | Block-scoped       |
| Hoisting       | Yes (undefined)    | Yes (TDZ)          | Yes (TDZ)          |
| Reassignment   | Yes                | Yes                | No                 |
| Re-declaration | Yes                | No                 | No                 |

**Temporal Dead Zone (TDZ):** `let` and `const` are hoisted but not initialized. Accessing them before declaration throws a `ReferenceError`.

### Syntax
```js
var   name = 'Alice';   // avoid
let   count = 0;        // use when value changes
const MAX   = 100;      // use by default
```

### Code Examples
```js
// var — function scoped, leaks out of blocks
function example() {
  if (true) {
    var x = 10;
  }
  console.log(x); // 10 — x leaks outside the if block
}

// let — block scoped
function example2() {
  if (true) {
    let y = 20;
  }
  console.log(y); // ReferenceError: y is not defined
}

// const — immutable binding (not immutable value)
const user = { name: 'Bob' };
user.name = 'Alice'; // ✅ allowed — object properties can change
user = {};           // ❌ TypeError — binding cannot change
```

### Internal Working
- JS engine performs two phases: **Creation Phase** (hoisting) and **Execution Phase**.
- `var` is initialized to `undefined` during creation phase.
- `let`/`const` are placed in TDZ — accessing them before initialization = `ReferenceError`.

### Edge Cases
```js
// TDZ trap
console.log(a); // ReferenceError
let a = 5;

// const with arrays/objects — mutable contents
const arr = [1, 2, 3];
arr.push(4);    // ✅ fine
arr = [5, 6];   // ❌ TypeError
```

### Common Mistakes
1. Using `var` in loops — causes closure bugs (see Module 5).
2. Confusing `const` as deeply immutable — it only freezes the binding, not the value.
3. Accessing `let`/`const` before declaration.

### Best Practices
- Default to `const`. Use `let` only when the variable needs to change.
- Never use `var` in modern code.
- Use `Object.freeze()` for truly immutable objects.

### Angular-Specific Relevance
```ts
// Angular component — const for injected services, let for mutable state
@Component({ ... })
export class UserComponent {
  readonly MAX_USERS = 100;          // const equivalent
  users: User[] = [];                // let equivalent for mutable array
}
```

### Interview Questions
1. What is the Temporal Dead Zone?
2. Why does `const` not make objects immutable?
3. What's the difference between block scope and function scope?
4. Why should we avoid `var`?

### Exercise
```
1. Predict the output:
   var a = 1;
   let b = 2;
   const c = 3;
   {
     var a = 10;
     let b = 20;
     console.log(a, b, c);
   }
   console.log(a, b, c);

2. Fix this bug caused by var in a loop (hint: use let):
   for (var i = 0; i < 3; i++) {
     setTimeout(() => console.log(i), 100);
   }
```

---

## 1.3 Data Types

### Simple Explanation
JavaScript has 8 data types — 7 primitives and 1 object type.

### Primitives (stored by value)
| Type        | Example              |
|-------------|----------------------|
| `string`    | `'hello'`            |
| `number`    | `42`, `3.14`, `NaN`  |
| `boolean`   | `true`, `false`      |
| `undefined` | uninitialized var    |
| `null`      | intentional absence  |
| `symbol`    | unique identifier    |
| `bigint`    | `9007199254740991n`  |

### Object type (stored by reference)
Everything else: plain objects `{}`, arrays `[]`, functions, `Date`, `Map`, `Set`, etc.

### Code Examples
```js
// Primitives — copied by value
let a = 5;
let b = a;
b = 10;
console.log(a); // 5 — unaffected

// Objects — copied by reference
let obj1 = { x: 1 };
let obj2 = obj1;
obj2.x = 99;
console.log(obj1.x); // 99 — same reference!
```

### Type Checking
```js
typeof 'hello'      // 'string'
typeof 42           // 'number'
typeof true         // 'boolean'
typeof undefined    // 'undefined'
typeof null         // 'object'  ← FAMOUS BUG in JS
typeof {}           // 'object'
typeof []           // 'object'  ← use Array.isArray() instead
typeof function(){} // 'function'
typeof Symbol()     // 'symbol'
typeof 1n           // 'bigint'

// Better checks
Array.isArray([])          // true
null === null              // true
obj instanceof SomeClass   // true/false
```

### Internal Working
- Primitives are stored directly on the **stack**.
- Objects are stored on the **heap**; variables hold a reference (pointer) to that heap location.

### Edge Cases
```js
typeof null    // 'object' — legacy bug, never fixed for backward compatibility
NaN === NaN    // false — NaN is the only value not equal to itself
Number.isNaN(NaN) // true — correct way to check

0.1 + 0.2 === 0.3  // false — floating point precision issue
(0.1 + 0.2).toFixed(1) === '0.3' // true — workaround
```

### Common Mistakes
1. Using `==` instead of `===` — `==` performs type coercion.
2. Checking `typeof null === 'object'` and treating null as object.
3. Mutating objects unknowingly due to reference sharing.

### Best Practices
- Always use `===` (strict equality).
- Use `Array.isArray()` to check arrays.
- Use `Number.isNaN()` for NaN checks.
- Clone objects before mutating: `{ ...obj }` or `structuredClone(obj)`.

### Angular-Specific Relevance
```ts
// Angular uses TypeScript's strict typing to prevent these pitfalls
user: User | null = null;     // explicit null handling
items: string[] = [];         // typed arrays prevent bugs
```

### Interview Questions
1. What is the difference between `null` and `undefined`?
2. Why is `typeof null === 'object'`?
3. What is the difference between `==` and `===`?
4. Why does `0.1 + 0.2 !== 0.3`?
5. How do you safely clone an object?

### Exercise
```
1. What does this print and why?
   console.log(null == undefined);    // ?
   console.log(null === undefined);   // ?
   console.log(NaN == NaN);           // ?

2. Fix the mutation bug:
   const original = { name: 'Alice', scores: [100, 90] };
   const copy = { ...original };
   copy.scores.push(80);
   console.log(original.scores); // what happens? how to fix?
```

---

## 1.4 Type Coercion & `==` vs `===`

### Simple Explanation
`==` converts types before comparing. `===` does not. Always use `===`.

### Deep Explanation
JavaScript uses abstract equality algorithm (`==`) which follows complex coercion rules. This leads to infamous surprising results.

### Code Examples
```js
// Type coercion surprises with ==
0 == false      // true — false converts to 0
'' == false     // true — both convert to 0
null == undefined // true — special rule
null == 0       // false — null only == undefined
[] == false     // true — [] → '' → 0, false → 0
[] == ![]       // true — this breaks brains

// Strict equality — no coercion
0 === false     // false
'' === false    // false
null === undefined // false
```

### The Falsy Values
```js
// These are ALL falsy in JS:
false, 0, -0, 0n, '', null, undefined, NaN

// Everything else is truthy, including:
'0', [], {}, -1, Infinity
```

### Common Mistakes
```js
// Checking truthiness — these are tricky
if ([]) console.log('truthy'); // ✅ prints — empty array is truthy!
if ({}) console.log('truthy'); // ✅ prints — empty object is truthy!
```

### Best Practices
- Always use `===`.
- Explicit boolean conversion: `Boolean(value)` or `!!value`.
- Be explicit about null/undefined checks: `value === null || value === undefined` or `value == null` (one rare valid use of `==`).

### Angular-Specific Relevance
```html
<!-- Angular template — falsy check with ngIf -->
<div *ngIf="user">...</div>        <!-- null, undefined, '' will hide -->
<div *ngIf="items.length">...</div> <!-- 0 hides, correct for empty array -->
```

---

## 1.5 Operators

### Arithmetic
```js
+ - * / % **   // addition, subtraction, multiply, divide, modulo, exponent
5 ** 2         // 25
10 % 3         // 1
```

### Comparison
```js
> < >= <= === !==   // always use === and !==
```

### Logical
```js
&&  // AND — returns first falsy or last value
||  // OR  — returns first truthy or last value
!   // NOT
??  // Nullish coalescing — returns right side only if left is null/undefined

// Short-circuit evaluation
const name = user && user.name;       // safe access
const label = name || 'Anonymous';   // fallback
const title = config ?? 'Default';   // only null/undefined triggers fallback
```

### Optional Chaining `?.`
```js
const street = user?.address?.street; // undefined if any part is null/undefined
const len = arr?.length;              // safe on potentially null array
const val = obj?.method?.();          // safe method call
```

### Nullish Coalescing vs OR
```js
const a = 0 || 'default';   // 'default' — 0 is falsy
const b = 0 ?? 'default';   // 0         — 0 is NOT null/undefined
```

### Angular-Specific Relevance
```html
<!-- Optional chaining in templates -->
{{ user?.profile?.avatar }}
{{ items?.length ?? 0 }}
```

---

## 1.6 Control Flow

### Conditionals
```js
// if / else if / else
if (score >= 90) {
  grade = 'A';
} else if (score >= 80) {
  grade = 'B';
} else {
  grade = 'C';
}

// Ternary — for simple inline conditions
const label = isLoggedIn ? 'Logout' : 'Login';

// Switch — for multiple discrete values
switch (role) {
  case 'admin':
    loadAdminDashboard();
    break;
  case 'user':
    loadUserDashboard();
    break;
  default:
    redirectToLogin();
}
```

### Loops
```js
// for — classic indexed iteration
for (let i = 0; i < 5; i++) { ... }

// for...of — iterate values (arrays, strings, Maps, Sets)
for (const item of items) { ... }

// for...in — iterate keys of an object (avoid for arrays)
for (const key in obj) { ... }

// while
while (condition) { ... }

// do...while — executes at least once
do { ... } while (condition);

// Array iteration methods (preferred — see Module 3)
items.forEach(item => ...);
items.map(item => ...);
items.filter(item => ...);
```

### Best Practices
- Prefer `for...of` over classic `for` loops for arrays.
- Avoid `for...in` on arrays — use `Object.keys()` for objects.
- Prefer array methods (`map`, `filter`, `reduce`) for data transformation (Module 3).

---

## 1.7 Functions (Introduction)

> Functions are deeply covered in Module 2. Here is the minimum to proceed.

```js
// Function declaration — hoisted
function greet(name) {
  return `Hello, ${name}`;
}

// Function expression — not hoisted
const greet = function(name) {
  return `Hello, ${name}`;
};

// Arrow function — concise, no own `this` (critical for Angular)
const greet = (name) => `Hello, ${name}`;
```

---

## 1.8 Template Literals

```js
const name = 'Alice';
const age = 30;

// Old way
console.log('Name: ' + name + ', Age: ' + age);

// Template literal
console.log(`Name: ${name}, Age: ${age}`);

// Multi-line
const html = `
  <div>
    <h1>${name}</h1>
    <p>Age: ${age}</p>
  </div>
`;

// Expression inside ${}
console.log(`Result: ${2 + 2}`);
console.log(`Status: ${isAdmin ? 'Admin' : 'User'}`);
```

---

## Module 1 — Summary Notes

| Concept                   | Key Takeaway                                                   |
|---------------------------|---------------------------------------------------------------|
| Variables                 | `const` by default; `let` for mutable; never `var`           |
| Data Types                | 7 primitives (by value) + objects (by reference)             |
| `typeof null`             | `'object'` — historic bug                                     |
| `==` vs `===`             | Always use `===`                                              |
| Falsy values              | `false, 0, '', null, undefined, NaN`                         |
| Optional chaining `?.`    | Safe deep access                                              |
| Nullish coalescing `??`   | Fallback only on `null`/`undefined`, not other falsy values  |
| Copy by reference         | Objects/arrays copied by reference — clone before mutating   |

---

## Module 1 — Checkpoint Tasks

Before moving on, complete these:

- [ ] Predict the output of 10 `==` comparisons using the coercion rules.
- [ ] Write a function that deep-clones a nested object without using a library.
- [ ] Fix a loop bug caused by `var` using `let`.
- [ ] Rewrite 3 nested `if`/`else` blocks using ternary and optional chaining.
- [ ] Identify all falsy values in a given mixed array using `filter(Boolean)`.

---

> **Module 1 complete.** Type `continue` to proceed to **Module 2 – Functions & Execution Context.**

---

# Module 2 – Functions & Execution Context

---

## 2.1 What is a Function?

### Simple Explanation
A function is a reusable block of code that performs a task and optionally returns a value.

### Deep Explanation
In JavaScript, functions are **first-class citizens** — they can be:
- Assigned to variables
- Passed as arguments
- Returned from other functions
- Stored in arrays or objects

This makes JavaScript extremely powerful for functional and reactive patterns — both critical in Angular (RxJS, event handling, DI factories).

---

## 2.2 Function Types

### Function Declaration
```js
function add(a, b) {
  return a + b;
}
```
- **Hoisted** — callable before its definition in the file.
- Has its own `this`.

### Function Expression
```js
const add = function(a, b) {
  return a + b;
};
```
- **Not hoisted** — must be defined before use.
- Has its own `this`.

### Arrow Function
```js
const add = (a, b) => a + b;
```
- **Not hoisted**.
- **No own `this`** — inherits `this` from enclosing lexical scope.
- Cannot be used as constructors.
- No `arguments` object.

### Method (function inside object)
```js
const obj = {
  name: 'Alice',
  greet() {             // shorthand method syntax
    return `Hi, ${this.name}`;
  }
};
```

### Constructor Function (pre-ES6)
```js
function User(name) {
  this.name = name;
}
const u = new User('Bob');
```

### Comparison Table

| Feature              | Declaration | Expression | Arrow   |
|----------------------|-------------|------------|---------|
| Hoisted              | ✅          | ❌         | ❌      |
| Own `this`           | ✅          | ✅         | ❌      |
| `arguments` object   | ✅          | ✅         | ❌      |
| Use as constructor   | ✅          | ✅         | ❌      |
| Implicit return      | ❌          | ❌         | ✅      |

---

## 2.3 Execution Context

### Simple Explanation
Every time JavaScript runs code, it creates an **Execution Context** — the environment in which the code runs.

### Deep Explanation
There are three types:

1. **Global Execution Context (GEC)** — created once when the script loads.
2. **Function Execution Context (FEC)** — created every time a function is called.
3. **Eval Execution Context** — (avoid eval entirely).

Each context has two phases:

#### Phase 1 — Creation Phase
- `this` binding is set.
- **Scope chain** is created.
- Variables are **hoisted** (`var` → `undefined`, `let`/`const` → TDZ).
- Function declarations are fully hoisted.

#### Phase 2 — Execution Phase
- Code runs line by line.
- Variables get their assigned values.

### Mental Model
```
┌─────────────────────────────────────────────┐
│           Global Execution Context          │
│  this = window (browser) / global (Node)    │
│  ┌─────────────────────────────────────┐    │
│  │     Function Execution Context      │    │
│  │  this = depends on how fn is called │    │
│  │  ┌───────────────────────────────┐  │    │
│  │  │  Nested Function EC           │  │    │
│  │  └───────────────────────────────┘  │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

---

## 2.4 The Call Stack

### Simple Explanation
The **Call Stack** is a data structure that tracks which function is currently running and where to return after it finishes.

### How It Works
```js
function third() {
  console.log('Third');
}

function second() {
  third();
  console.log('Second');
}

function first() {
  second();
  console.log('First');
}

first();
```

**Call Stack trace:**
```
1. first() pushed
2.   second() pushed
3.     third() pushed
4.     third() popped — logs 'Third'
5.   second() resumes — logs 'Second' — popped
6. first() resumes — logs 'First' — popped
```

### Stack Overflow
```js
function infinite() {
  return infinite(); // no base case
}
infinite(); // RangeError: Maximum call stack size exceeded
```

### Angular Relevance
- Angular's change detection, lifecycle hooks, and DI resolution all execute via the call stack.
- Understanding the stack helps debug cryptic stack traces in Angular error messages.

---

## 2.5 `this` Keyword

### Simple Explanation
`this` refers to the object that is currently executing the function. Its value depends entirely on **how** the function is called, not where it is defined — except for arrow functions.

### The 5 Rules of `this`

#### Rule 1 — Global context
```js
console.log(this); // window (browser) | global (Node) | {} in strict mode
```

#### Rule 2 — Object method call
```js
const user = {
  name: 'Alice',
  greet() {
    console.log(this.name); // 'Alice' — this = user object
  }
};
user.greet();
```

#### Rule 3 — Regular function call (standalone)
```js
function show() {
  console.log(this); // window (sloppy) | undefined (strict mode)
}
show();
```

#### Rule 4 — `new` keyword
```js
function Person(name) {
  this.name = name;  // this = newly created object
}
const p = new Person('Bob');
console.log(p.name); // 'Bob'
```

#### Rule 5 — Explicit binding: `call`, `apply`, `bind`
```js
function greet() {
  return `Hello, ${this.name}`;
}

const user = { name: 'Alice' };

greet.call(user);         // 'Hello, Alice' — calls immediately
greet.apply(user);        // 'Hello, Alice' — calls immediately (args as array)
const bound = greet.bind(user);
bound();                  // 'Hello, Alice' — returns new bound function
```

#### Arrow function — Rule 0 (overrides everything)
```js
const timer = {
  name: 'Timer',
  start() {
    setTimeout(() => {
      console.log(this.name); // 'Timer' — arrow inherits this from start()
    }, 1000);
  }
};
timer.start();
```

### The Classic `this` Bug
```js
const user = {
  name: 'Alice',
  greet() {
    setTimeout(function() {
      console.log(this.name); // undefined! — regular fn loses this
    }, 1000);
  }
};

// Fix 1 — arrow function
greet() {
  setTimeout(() => {
    console.log(this.name); // ✅ 'Alice'
  }, 1000);
}

// Fix 2 — bind
greet() {
  setTimeout(function() {
    console.log(this.name); // ✅ 'Alice'
  }.bind(this), 1000);
}
```

### Angular-Specific Relevance
```ts
// Angular uses arrow functions in class methods to preserve this
@Component({ ... })
export class AppComponent {
  title = 'My App';

  // ✅ Arrow function preserves 'this' in callbacks
  handleData = (data: any) => {
    console.log(this.title); // works correctly
  };

  ngOnInit() {
    this.dataService.getData().subscribe(this.handleData);
  }
}
```

---

## 2.6 Parameters & Arguments

### Default Parameters
```js
function createUser(name, role = 'user', active = true) {
  return { name, role, active };
}

createUser('Alice');                // { name: 'Alice', role: 'user', active: true }
createUser('Bob', 'admin');        // { name: 'Bob', role: 'admin', active: true }
createUser('Eve', 'user', false);  // { name: 'Eve', role: 'user', active: false }
```

### Rest Parameters (`...args`)
```js
function sum(...numbers) {
  return numbers.reduce((acc, n) => acc + n, 0);
}

sum(1, 2, 3, 4, 5); // 15
```

### Spread Operator (related concept)
```js
const nums = [1, 2, 3];
console.log(Math.max(...nums)); // 3 — spreads array into args

const arr1 = [1, 2];
const arr2 = [3, 4];
const merged = [...arr1, ...arr2]; // [1, 2, 3, 4]
```

### Destructuring Parameters
```js
// Object destructuring in params
function displayUser({ name, age, role = 'user' }) {
  console.log(`${name} (${age}) — ${role}`);
}

displayUser({ name: 'Alice', age: 30 }); // 'Alice (30) — user'

// Array destructuring in params
function getFirst([first, second]) {
  return first;
}
```

---

## 2.7 Hoisting — Deep Dive

### Simple Explanation
Hoisting is JavaScript's behavior of moving declarations to the top of their scope during the creation phase.

### What Gets Hoisted?

```js
// 1. Function declarations — FULLY hoisted (name + body)
hello(); // ✅ works — 'Hello'
function hello() { console.log('Hello'); }

// 2. var — hoisted as undefined
console.log(x); // undefined (not error)
var x = 5;
console.log(x); // 5

// 3. let/const — hoisted but TDZ
console.log(y); // ❌ ReferenceError
let y = 10;

// 4. Function expressions — NOT hoisted
greet(); // ❌ TypeError: greet is not a function
var greet = function() { return 'hi'; };
// (var is hoisted as undefined, calling undefined() = TypeError)
```

### Mental Model
> During the **Creation Phase**, JavaScript reads the entire scope and registers all declarations. `var` gets a placeholder of `undefined`. `let`/`const` get registered but locked in TDZ. Function declarations get their full definition loaded immediately.

### Common Interview Trap
```js
var x = 1;
function test() {
  console.log(x); // undefined — NOT 1
  var x = 2;
  console.log(x); // 2
}
test();
// The inner var x is hoisted within test() scope, shadowing global x
```

---

## 2.8 Pure Functions & Side Effects

### Pure Function
A function is **pure** if:
1. Given the same inputs, always returns the same output.
2. Has no side effects (doesn't modify external state).

```js
// Pure ✅
function add(a, b) {
  return a + b;
}

// Impure ❌ — modifies external state
let total = 0;
function addToTotal(n) {
  total += n; // side effect
}

// Impure ❌ — relies on external state
function getFullName() {
  return `${globalUser.first} ${globalUser.last}`; // external dependency
}
```

### Why It Matters for Angular
- **Angular services** should ideally be pure — same input → same output.
- **Pipes** must be pure (Angular calls them only when inputs change).
- **RxJS operators** like `map`, `filter` are pure.
- Pure functions are easily testable, predictable, and cacheable.

---

## 2.9 Immediately Invoked Function Expressions (IIFE)

```js
// Classic IIFE — creates a private scope
(function() {
  const secret = 'hidden';
  console.log(secret); // 'hidden'
})();

console.log(secret); // ReferenceError — not accessible outside

// Arrow IIFE
(() => {
  console.log('IIFE arrow');
})();

// IIFE with return value
const result = (() => {
  const x = 10;
  return x * 2;
})();
console.log(result); // 20
```

### Modern Usage
IIFEs are less common today (modules provide scope isolation), but you'll encounter them in:
- Legacy codebases
- Polyfills
- Angular's compiled output (AOT compilation wraps modules in IIFEs)

---

## 2.10 Higher-Order Functions

### Simple Explanation
A **higher-order function** is a function that takes another function as an argument, or returns a function.

```js
// Takes a function as argument
function repeat(fn, times) {
  for (let i = 0; i < times; i++) fn(i);
}
repeat(i => console.log(`Step ${i}`), 3);

// Returns a function
function multiplier(factor) {
  return (number) => number * factor;
}
const double = multiplier(2);
const triple = multiplier(3);
double(5);  // 10
triple(5);  // 15

// Real-world: event handling
button.addEventListener('click', () => console.log('clicked'));
// addEventListener is a higher-order function
```

### Angular Relevance
Higher-order functions are everywhere in Angular:
```ts
// RxJS pipe uses higher-order functions
this.users$ = this.http.get<User[]>('/api/users').pipe(
  map(users => users.filter(u => u.active)),   // map is HOF
  catchError(err => of([]))                    // catchError is HOF
);

// Route guards
canActivate: [() => inject(AuthService).isLoggedIn()]
```

---

## Module 2 — Summary Notes

| Concept              | Key Takeaway                                                                     |
|----------------------|---------------------------------------------------------------------------------|
| Function types       | Declaration = hoisted; Expression & Arrow = not hoisted                        |
| Arrow vs Regular     | Arrow has no own `this` — inherits from enclosing scope                        |
| Execution Context    | Created for global scope + every function call; has creation + execution phase |
| Call Stack           | LIFO stack tracking current execution                                           |
| `this` — 5 rules     | Depends on call site; arrow always inherits lexically                          |
| `call`/`apply`/`bind`| Explicitly set `this`; `bind` returns new function                             |
| Hoisting             | `var` → `undefined`; `let`/`const` → TDZ; fn declarations → fully hoisted     |
| Pure functions       | Same input → same output; no side effects                                      |
| Higher-order fn      | Takes or returns a function — foundation of RxJS and functional patterns       |

---

## Module 2 — Checkpoint Tasks

- [ ] Explain the output of a 5-level nested function call stack, step by step.
- [ ] Write a `bind` polyfill from scratch.
- [ ] Create a `makeCounter()` factory function using closures (preview of Module 5).
- [ ] Identify which functions are pure and which are impure in a given code block.
- [ ] Rewrite a class method that loses `this` in a callback using an arrow function fix.
- [ ] Write a higher-order function `memoize(fn)` that caches results.

---

> **Module 2 complete.** Type `continue` to proceed to **Module 3 – Arrays Mastery.**

---

# Module 3 – Arrays Mastery

---

## 3.1 What is an Array?

### Simple Explanation
An array is an ordered, indexed collection of values. In JavaScript, arrays are objects under the hood — they can hold any mix of types.

### Deep Explanation
- Arrays are **zero-indexed**.
- They are **dynamic** — no fixed size.
- Internally, JS engines optimize dense arrays (sequential numeric keys) differently from sparse arrays.
- `typeof []` returns `'object'` — use `Array.isArray()` to check.

```js
const arr = [1, 'hello', true, null, { id: 1 }, [2, 3]];
console.log(arr.length);       // 6
console.log(arr[4]);           // { id: 1 }
console.log(Array.isArray(arr)); // true
```

### Mental Model
> An array is a **numbered shelf**. Each slot has a position (index), and you can put anything on any shelf — even another shelf (nested arrays).

---

## 3.2 Creating Arrays

```js
// Literal — preferred
const arr = [1, 2, 3];

// Array constructor — avoid (confusing with single arg)
new Array(3);        // [ <3 empty slots> ] — sparse!
new Array(1, 2, 3);  // [1, 2, 3]

// Array.of — fixes the single-arg confusion
Array.of(3);         // [3]
Array.of(1, 2, 3);   // [1, 2, 3]

// Array.from — create from iterable or array-like
Array.from('hello');              // ['h','e','l','l','o']
Array.from({ length: 5 }, (_, i) => i); // [0,1,2,3,4]
Array.from(new Set([1,2,2,3]));   // [1,2,3]
Array.from(new Map([['a',1]]));   // [['a',1]]
```

### Angular Relevance
```ts
// Array.from used to convert QueryList or NodeList
Array.from(this.elementRef.nativeElement.querySelectorAll('input'));
```

---

## 3.3 Core Mutating Methods

These methods **change the original array**.

```js
const arr = [1, 2, 3];

// push / pop — end of array
arr.push(4);       // [1,2,3,4] — returns new length
arr.pop();         // [1,2,3]   — returns removed element

// unshift / shift — start of array
arr.unshift(0);    // [0,1,2,3] — returns new length
arr.shift();       // [1,2,3]   — returns removed element

// splice — remove/insert at any position
arr.splice(1, 1);         // removes 1 element at index 1 → [1,3]
arr.splice(1, 0, 2);      // inserts 2 at index 1 → [1,2,3]
arr.splice(1, 1, 99);     // replaces index 1 with 99 → [1,99,3]

// sort — mutates! sorts lexicographically by default
[10, 2, 1].sort();                     // [1, 10, 2] ← wrong for numbers!
[10, 2, 1].sort((a, b) => a - b);     // [1, 2, 10] ← correct ascending
[10, 2, 1].sort((a, b) => b - a);     // [10, 2, 1] ← descending

// reverse — mutates
[1, 2, 3].reverse(); // [3, 2, 1]

// fill
[1, 2, 3, 4].fill(0, 1, 3); // [1, 0, 0, 4]
```

### Best Practice
> Avoid mutating arrays in Angular — always return new arrays to enable change detection.

---

## 3.4 Core Non-Mutating Methods

These methods **return a new array or value**, leaving the original unchanged. These are your primary tools in Angular.

### `map` — transform each element
```js
const nums = [1, 2, 3, 4];
const doubled = nums.map(n => n * 2);  // [2, 4, 6, 8]

// Real-world: transform API data
const users = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }];
const names = users.map(u => u.name);   // ['Alice', 'Bob']
const withRole = users.map(u => ({ ...u, role: 'user' }));
```

### `filter` — select elements matching a condition
```js
const nums = [1, 2, 3, 4, 5, 6];
const evens = nums.filter(n => n % 2 === 0);  // [2, 4, 6]

// Real-world
const activeUsers = users.filter(u => u.active);
const admins = users.filter(u => u.role === 'admin');
```

### `reduce` — accumulate to a single value
```js
const nums = [1, 2, 3, 4, 5];

// Sum
const sum = nums.reduce((acc, n) => acc + n, 0);  // 15

// Build an object from array
const users = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }];
const byId = users.reduce((acc, user) => {
  acc[user.id] = user;
  return acc;
}, {});
// { 1: { id:1, name:'Alice' }, 2: { id:2, name:'Bob' } }

// Flatten nested arrays (before flat())
[[1,2],[3,4]].reduce((acc, arr) => acc.concat(arr), []); // [1,2,3,4]
```

### `find` / `findIndex` — first match
```js
const users = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }];

users.find(u => u.id === 2);        // { id: 2, name: 'Bob' }
users.findIndex(u => u.id === 2);   // 1
users.find(u => u.id === 99);       // undefined
```

### `some` / `every` — boolean tests
```js
const nums = [1, 2, 3, 4, 5];

nums.some(n => n > 4);    // true  — at least one matches
nums.every(n => n > 0);   // true  — all match
nums.every(n => n > 3);   // false — not all match
```

### `includes` / `indexOf`
```js
[1, 2, 3].includes(2);      // true
[1, 2, 3].indexOf(2);       // 1
[1, 2, 3].indexOf(99);      // -1
[1, NaN].includes(NaN);     // true  ← includes handles NaN
[1, NaN].indexOf(NaN);      // -1   ← indexOf does NOT handle NaN
```

### `slice` — copy a portion
```js
const arr = [1, 2, 3, 4, 5];
arr.slice(1, 3);    // [2, 3] — from index 1, up to (not including) 3
arr.slice(-2);      // [4, 5] — last 2 elements
arr.slice();        // [1,2,3,4,5] — shallow copy of entire array
```

### `concat` — merge arrays
```js
[1, 2].concat([3, 4], [5]); // [1, 2, 3, 4, 5]
// Prefer spread:
[...[1,2], ...[3,4]];       // [1, 2, 3, 4]
```

### `flat` / `flatMap`
```js
[1, [2, 3], [4, [5]]].flat();    // [1, 2, 3, 4, [5]] — 1 level
[1, [2, 3], [4, [5]]].flat(2);   // [1, 2, 3, 4, 5]   — 2 levels
[1, [2, 3], [4, [5]]].flat(Infinity); // fully flat

// flatMap = map + flat(1) in one pass
const sentences = ['Hello world', 'Foo bar'];
sentences.flatMap(s => s.split(' ')); // ['Hello','world','Foo','bar']
```

### `forEach` — iterate, no return value
```js
[1, 2, 3].forEach((item, index) => {
  console.log(index, item);
});
// Use map/filter/reduce instead when you need a result
```

---

## 3.5 Array Destructuring

```js
const [first, second, ...rest] = [1, 2, 3, 4, 5];
// first = 1, second = 2, rest = [3,4,5]

// Skip elements
const [,, third] = [10, 20, 30];
// third = 30

// Default values
const [a = 0, b = 0] = [1];
// a = 1, b = 0

// Swap variables
let x = 1, y = 2;
[x, y] = [y, x];
// x = 2, y = 1

// From function return
function getCoords() { return [40.7, -74.0]; }
const [lat, lng] = getCoords();
```

### Angular Relevance
```ts
// RxJS — destructure emitted arrays
this.route.params.subscribe(({ id }) => {
  this.loadUser(id);
});

// Angular animations state change
transition('* => *', [
  // ...
])
```

---

## 3.6 Sorting — Deep Dive

```js
// Default sort converts to strings — WRONG for numbers
[100, 20, 3].sort();                   // [100, 20, 3] — lexicographic!

// Correct numeric sort
[100, 20, 3].sort((a, b) => a - b);   // [3, 20, 100] ascending
[100, 20, 3].sort((a, b) => b - a);   // [100, 20, 3] descending

// Sort strings
['banana','apple','cherry'].sort((a, b) => a.localeCompare(b));
// ['apple','banana','cherry']

// Sort objects by property
const users = [{ name: 'Charlie' }, { name: 'Alice' }, { name: 'Bob' }];
users.sort((a, b) => a.name.localeCompare(b.name));

// Stable sort (guaranteed in modern JS engines, ES2019+)
// Equal elements maintain their original order
```

---

## 3.7 Immutable Array Patterns (Critical for Angular)

Angular's change detection relies on **reference equality**. Mutating an array in-place won't trigger change detection — always return a new array.

```js
const users = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }];

// ❌ BAD — mutates original, Angular won't detect change
users.push({ id: 3, name: 'Charlie' });

// ✅ GOOD — new array reference
const updatedUsers = [...users, { id: 3, name: 'Charlie' }];

// Remove item immutably
const withoutBob = users.filter(u => u.id !== 2);

// Update item immutably
const withUpdatedAlice = users.map(u =>
  u.id === 1 ? { ...u, name: 'Alicia' } : u
);

// Insert at index immutably
const insertAt = (arr, index, item) => [
  ...arr.slice(0, index),
  item,
  ...arr.slice(index)
];
```

---

## 3.8 Searching & Transforming — Chaining Methods

```js
const orders = [
  { id: 1, user: 'Alice', total: 250, status: 'completed' },
  { id: 2, user: 'Bob',   total: 90,  status: 'pending'   },
  { id: 3, user: 'Alice', total: 400, status: 'completed' },
  { id: 4, user: 'Charlie', total: 50, status: 'cancelled'},
];

// Get total revenue from Alice's completed orders
const aliceRevenue = orders
  .filter(o => o.user === 'Alice' && o.status === 'completed')
  .map(o => o.total)
  .reduce((sum, total) => sum + total, 0);
// 650

// Get unique users
const uniqueUsers = [...new Set(orders.map(o => o.user))];
// ['Alice', 'Bob', 'Charlie']

// Group by status
const grouped = orders.reduce((acc, order) => {
  (acc[order.status] ??= []).push(order);
  return acc;
}, {});
```

---

## 3.9 Advanced: `Array.from` Patterns

```js
// Create range
const range = (start, end) =>
  Array.from({ length: end - start }, (_, i) => start + i);
range(1, 6); // [1, 2, 3, 4, 5]

// Create matrix
const matrix = Array.from({ length: 3 }, () => Array(3).fill(0));
// [[0,0,0],[0,0,0],[0,0,0]]

// Convert Set to Array
const unique = Array.from(new Set([1, 2, 2, 3, 3])); // [1, 2, 3]

// Convert Map entries
const map = new Map([['a', 1], ['b', 2]]);
Array.from(map.entries()); // [['a',1],['b',2]]
Array.from(map.keys());    // ['a','b']
Array.from(map.values());  // [1, 2]
```

---

## 3.10 Common Mistakes & Edge Cases

```js
// 1. sort() without comparator
[1, 10, 100].sort();             // [1, 10, 100] — accident correct
[1, 10, 9].sort();               // [1, 10, 9] ← WRONG: '10' < '9' lexicographically

// 2. forEach returns undefined — don't chain it
const result = [1,2,3].forEach(x => x * 2); // undefined!

// 3. find vs filter — find returns first item, filter returns array
[1,2,3].find(x => x > 1);    // 2 — first match
[1,2,3].filter(x => x > 1);  // [2, 3] — all matches

// 4. Holes in sparse arrays
const sparse = [1, , 3];
sparse.length;          // 3
sparse[1];              // undefined
sparse.map(x => x * 2); // [2, empty, 6] — skips holes

// 5. Mutating inside map — bad practice
const users = [{ name: 'Alice' }];
users.map(u => {
  u.active = true; // ❌ mutates original
  return u;
});
// Fix:
users.map(u => ({ ...u, active: true })); // ✅ new object
```

---

## Module 3 — Summary Notes

| Method          | Mutates? | Returns         | Use For                              |
|-----------------|----------|-----------------|--------------------------------------|
| `push/pop`      | ✅       | element/length  | Stack operations                     |
| `shift/unshift` | ✅       | element/length  | Queue operations                     |
| `splice`        | ✅       | removed items   | Insert/remove at index               |
| `sort`          | ✅       | sorted array    | Sorting (always provide comparator)  |
| `map`           | ❌       | new array       | Transform each element               |
| `filter`        | ❌       | new array       | Select elements by condition         |
| `reduce`        | ❌       | single value    | Aggregate/accumulate                 |
| `find`          | ❌       | element/undef   | First matching element               |
| `some/every`    | ❌       | boolean         | Existence/all-match checks           |
| `slice`         | ❌       | new array       | Copy a portion                       |
| `flat/flatMap`  | ❌       | new array       | Flatten nested arrays                |
| `includes`      | ❌       | boolean         | Membership check                     |
| `forEach`       | ❌       | undefined       | Side effects only (prefer map etc.)  |

---

## Module 3 — Checkpoint Tasks

- [ ] Implement `groupBy(arr, key)` using `reduce` — groups array of objects by a property.
- [ ] Write `unique(arr)` that removes duplicates using `Set` and `Array.from`.
- [ ] Implement `flatten(arr)` that deeply flattens nested arrays without using `flat()`.
- [ ] Given a list of orders, compute total revenue per user using `reduce`.
- [ ] Rewrite a component that mutates an array to use immutable patterns.
- [ ] Sort an array of users by last name, then by first name for ties.

---

> **Module 3 complete.** Type `continue` to proceed to **Module 4 – Objects Mastery.**

---

# Module 4 – Objects Mastery

---

## 4.1 What is an Object?

### Simple Explanation
An object is a collection of **key-value pairs** used to represent real-world entities. Keys are strings (or Symbols), values can be anything.

### Deep Explanation
- Objects are the primary data structure in JavaScript.
- Everything that isn't a primitive **is** an object (arrays, functions, dates, etc.).
- Objects are **passed by reference** — crucial to understand for Angular's change detection.
- Properties can be **enumerable**, **configurable**, and **writable** — controlled via `Object.defineProperty`.

### Mental Model
> An object is a **named container** — like a labeled filing cabinet where each drawer (key) holds a value.

---

## 4.2 Creating Objects

```js
// Object literal — preferred
const user = {
  id: 1,
  name: 'Alice',
  active: true,
  greet() {
    return `Hello, ${this.name}`;
  }
};

// Constructor function (pre-ES6)
function User(name) {
  this.name = name;
}
const u = new User('Bob');

// ES6 Class (syntactic sugar over prototype)
class User {
  constructor(name) {
    this.name = name;
  }
}

// Object.create — set explicit prototype
const proto = { greet() { return `Hi, ${this.name}`; } };
const user = Object.create(proto);
user.name = 'Alice';
user.greet(); // 'Hi, Alice'

// Factory function (no new — preferred in functional style)
function createUser(name, role = 'user') {
  return { name, role, createdAt: Date.now() };
}
```

---

## 4.3 Property Access & Shorthand

```js
const name = 'Alice';
const role = 'admin';

// Shorthand property (ES6)
const user = { name, role };  // same as { name: name, role: role }

// Computed property names
const key = 'email';
const config = {
  [key]: 'alice@example.com',            // dynamic key
  [`${key}_verified`]: true,             // template literal key
};

// Property access
user.name;        // dot notation — use when key is known
user['name'];     // bracket notation — use when key is dynamic

// Dynamic key access
const field = 'name';
user[field];      // 'Alice'

// Nested access
const profile = { address: { city: 'NYC' } };
profile.address.city;           // 'NYC'
profile?.address?.city;         // 'NYC' — safe with optional chaining
profile?.contact?.phone;        // undefined — no error
```

---

## 4.4 Object Destructuring

```js
const user = { id: 1, name: 'Alice', role: 'admin', active: true };

// Basic destructuring
const { name, role } = user;

// Rename while destructuring
const { name: userName, role: userRole } = user;

// Default values
const { name, badge = 'none' } = user;   // badge = 'none' (not in object)

// Rest in destructuring
const { id, ...rest } = user;
// id = 1, rest = { name: 'Alice', role: 'admin', active: true }

// Nested destructuring
const config = { db: { host: 'localhost', port: 5432 } };
const { db: { host, port } } = config;

// In function parameters
function displayUser({ name, role = 'user', active }) {
  console.log(`${name} | ${role} | ${active}`);
}
displayUser(user);

// Combine with rename + default in params
function init({ host = 'localhost', port = 3000, debug = false } = {}) {
  // = {} default prevents crash if called with no args
}
```

### Angular Relevance
```ts
// Destructuring in Angular component inputs
@Component({...})
export class CardComponent {
  @Input() set config({ title, subtitle = '', actions = [] }: CardConfig) {
    this.title = title;
    this.subtitle = subtitle;
    this.actions = actions;
  }
}

// Destructuring route params
this.route.params.subscribe(({ id, section }) => {
  this.loadSection(id, section);
});
```

---

## 4.5 Spread & Object Composition

```js
const defaults = { theme: 'light', lang: 'en', notifications: true };
const userPrefs = { theme: 'dark', lang: 'fr' };

// Merge objects — later keys win
const config = { ...defaults, ...userPrefs };
// { theme: 'dark', lang: 'fr', notifications: true }

// Clone (shallow)
const clone = { ...original };

// Add/override properties immutably
const updated = { ...user, role: 'admin', updatedAt: Date.now() };

// Remove property immutably (rest destructuring)
const { password, ...safeUser } = user;  // exclude password

// Deep clone — spread only clones one level
const deep = { a: { b: 1 } };
const shallowClone = { ...deep };
shallowClone.a.b = 99;
console.log(deep.a.b);  // 99 — still shared reference!

// True deep clone options:
const deepClone = structuredClone(deep);          // modern, built-in
const deepClone2 = JSON.parse(JSON.stringify(deep)); // only for JSON-safe data
```

### Angular Relevance
```ts
// NgRx / state management — always return new object
on(updateUser, (state, { user }) => ({
  ...state,
  users: state.users.map(u => u.id === user.id ? { ...u, ...user } : u)
}));
```

---

## 4.6 Object Methods

### `Object.keys` / `Object.values` / `Object.entries`
```js
const user = { id: 1, name: 'Alice', role: 'admin' };

Object.keys(user);    // ['id', 'name', 'role']
Object.values(user);  // [1, 'Alice', 'admin']
Object.entries(user); // [['id',1], ['name','Alice'], ['role','admin']]

// Iterate over object
for (const [key, value] of Object.entries(user)) {
  console.log(`${key}: ${value}`);
}

// Transform object values
const upperValues = Object.fromEntries(
  Object.entries(user).map(([k, v]) => [k, String(v).toUpperCase()])
);
```

### `Object.assign`
```js
// Merge into target (mutates target)
const target = { a: 1 };
Object.assign(target, { b: 2 }, { c: 3 });
// target = { a:1, b:2, c:3 }

// Shallow clone (prefer spread instead)
const clone = Object.assign({}, source);
```

### `Object.freeze` / `Object.seal`
```js
const config = Object.freeze({ api: 'https://api.example.com', timeout: 3000 });
config.timeout = 5000;   // silently fails (throws in strict mode)
config.timeout;          // 3000 — unchanged

// freeze is shallow
const obj = Object.freeze({ nested: { x: 1 } });
obj.nested.x = 99;       // ✅ works — nested is NOT frozen

// seal — can update existing, can't add/delete
const sealed = Object.seal({ x: 1 });
sealed.x = 2;   // ✅ OK
sealed.y = 3;   // ❌ silently fails
```

### `Object.fromEntries`
```js
// Convert entries back to object
const entries = [['name', 'Alice'], ['role', 'admin']];
Object.fromEntries(entries); // { name: 'Alice', role: 'admin' }

// Convert Map to object
const map = new Map([['a', 1], ['b', 2]]);
Object.fromEntries(map);     // { a: 1, b: 2 }

// Transform object (entries → map → fromEntries pattern)
const prices = { apple: 1.5, banana: 0.5, cherry: 3.0 };
const discounted = Object.fromEntries(
  Object.entries(prices).map(([k, v]) => [k, v * 0.9])
);
```

### `Object.hasOwn` (modern) / `hasOwnProperty` (legacy)
```js
const user = { name: 'Alice' };

Object.hasOwn(user, 'name');         // true  — preferred (ES2022)
user.hasOwnProperty('name');         // true  — legacy
user.hasOwnProperty('toString');     // false — inherited, not own
'name' in user;                      // true  — includes inherited
```

---

## 4.7 Property Descriptors

### Deep Dive into Property Attributes
Every property has hidden attributes:
- **`value`** — the property's value
- **`writable`** — can the value be changed?
- **`enumerable`** — does it show in `for...in` / `Object.keys`?
- **`configurable`** — can the property be deleted or redefined?

```js
const obj = {};

Object.defineProperty(obj, 'id', {
  value: 42,
  writable: false,      // read-only
  enumerable: true,     // shows in Object.keys
  configurable: false   // can't delete or redefine
});

obj.id = 99;           // silently fails (throws in strict mode)
obj.id;                // 42

// Check descriptors
Object.getOwnPropertyDescriptor(obj, 'id');
// { value: 42, writable: false, enumerable: true, configurable: false }
```

### Getters & Setters
```js
const user = {
  firstName: 'Alice',
  lastName: 'Smith',

  get fullName() {
    return `${this.firstName} ${this.lastName}`;
  },

  set fullName(value) {
    [this.firstName, this.lastName] = value.split(' ');
  }
};

user.fullName;           // 'Alice Smith'
user.fullName = 'Bob Jones';
user.firstName;          // 'Bob'
```

### Angular Relevance
```ts
// Angular uses getters/setters for @Input with transformation
private _items: Item[] = [];

@Input()
set items(value: Item[]) {
  this._items = value.map(item => ({ ...item, processed: true }));
}
get items(): Item[] {
  return this._items;
}
```

---

## 4.8 Prototypes & Inheritance (Preview — Deep in Module 7)

```js
// Every object has a prototype chain
const arr = [1, 2, 3];
// arr → Array.prototype → Object.prototype → null

// Own vs inherited properties
function Animal(name) { this.name = name; }
Animal.prototype.speak = function() { return `${this.name} makes a sound`; };

const dog = new Animal('Rex');
dog.name;                        // 'Rex'   — own property
dog.speak();                     // 'Rex makes a sound' — inherited
Object.hasOwn(dog, 'name');      // true
Object.hasOwn(dog, 'speak');     // false — on prototype
```

---

## 4.9 Map & Set — Modern Alternatives

### `Map` — key-value store with any key type
```js
const map = new Map();
map.set('name', 'Alice');
map.set(1, 'one');
map.set({ id: 1 }, 'user object key');  // objects as keys!

map.get('name');    // 'Alice'
map.has('name');    // true
map.size;           // 3
map.delete('name');

// Iteration
for (const [key, value] of map) { ... }
map.forEach((value, key) => { ... });

// Convert to/from object
const obj = Object.fromEntries(map);
const map2 = new Map(Object.entries(obj));
```

**When to use Map over Object:**
- Keys are not strings/symbols.
- Frequent addition/deletion (Maps are more performant).
- Need insertion-order iteration.
- Need `.size`.

### `Set` — unique values collection
```js
const set = new Set([1, 2, 2, 3, 3, 3]);
set.size;        // 3
set.has(2);      // true
set.add(4);
set.delete(1);

// Deduplicate array — most common use
const unique = [...new Set([1, 2, 2, 3])]; // [1, 2, 3]

// Set operations
const a = new Set([1, 2, 3]);
const b = new Set([2, 3, 4]);

// Union
const union = new Set([...a, ...b]);              // {1,2,3,4}
// Intersection
const intersection = new Set([...a].filter(x => b.has(x))); // {2,3}
// Difference
const difference = new Set([...a].filter(x => !b.has(x)));  // {1}
```

### Angular Relevance
```ts
// Track selected items with Set
selectedIds = new Set<number>();

toggle(id: number) {
  if (this.selectedIds.has(id)) {
    this.selectedIds.delete(id);
  } else {
    this.selectedIds.add(id);
  }
  // Create new Set for change detection
  this.selectedIds = new Set(this.selectedIds);
}
```

---

## 4.10 WeakMap & WeakRef (Advanced)

```js
// WeakMap — keys must be objects, not enumerable, values GC'd with key
const cache = new WeakMap();

function processUser(user) {
  if (cache.has(user)) return cache.get(user);
  const result = expensiveOperation(user);
  cache.set(user, result);
  return result;
}
// When 'user' object is garbage collected, cache entry auto-removed
// Use: private data, memoization, DOM node metadata

// WeakSet — like Set but holds weak references to objects
const visitedNodes = new WeakSet();
```

---

## 4.11 Immutable Object Patterns (Critical for Angular)

```js
// ❌ Mutating — Angular won't detect as changed
this.user.name = 'Bob';
this.items.push(newItem);

// ✅ Immutable — new object reference triggers change detection
this.user = { ...this.user, name: 'Bob' };
this.items = [...this.items, newItem];

// Nested immutable update
this.config = {
  ...this.config,
  db: {
    ...this.config.db,
    host: 'new-host'
  }
};

// Remove key immutably
const { unwantedKey, ...cleanObject } = this.config;
this.config = cleanObject;
```

---

## 4.12 Common Mistakes & Edge Cases

```js
// 1. Shallow clone pitfall
const a = { x: { y: 1 } };
const b = { ...a };
b.x.y = 99;
console.log(a.x.y); // 99 — same nested reference!

// 2. for...in includes inherited properties
function Base() {}
Base.prototype.inherited = true;
const obj = new Base();
obj.own = true;
for (const key in obj) {
  console.log(key); // 'own', 'inherited'
}
// Fix: use Object.keys() or check hasOwn
for (const key of Object.keys(obj)) {
  console.log(key); // 'own' only
}

// 3. JSON.stringify loses functions, undefined, Symbol
const obj = { a: 1, fn: () => {}, b: undefined, s: Symbol() };
JSON.stringify(obj); // '{"a":1}' — fn, b, s silently dropped

// 4. Object comparison by reference
{ a: 1 } === { a: 1 }  // false — different references
// Use JSON.stringify for simple cases or deep-equal libraries
```

---

## Module 4 — Summary Notes

| Concept                  | Key Takeaway                                                                  |
|--------------------------|------------------------------------------------------------------------------|
| Object literal           | Most common creation method; shorthand properties + computed keys           |
| Destructuring            | Extract with rename, defaults, rest, and nested patterns                    |
| Spread `{...obj}`        | Shallow clone; merge; immutable update                                       |
| `Object.keys/values/entries` | Iterate or transform object properties                                  |
| `Object.freeze`          | Shallow immutability; nested objects still mutable                         |
| Getters/Setters          | Computed properties with logic; used in Angular `@Input`                   |
| `Map` vs Object          | Map: any key type, ordered, `.size`, better perf for dynamic keys          |
| `Set`                    | Unique values; deduplicate arrays; set operations                          |
| Immutable patterns       | Always return new objects for Angular change detection                      |
| Reference equality       | Objects compared by reference — `{} !== {}` even with same content         |

---

## Module 4 — Checkpoint Tasks

- [ ] Write `deepClone(obj)` without using `structuredClone` or `JSON.parse`.
- [ ] Implement `pick(obj, keys)` — returns new object with only specified keys.
- [ ] Implement `omit(obj, keys)` — returns new object without specified keys.
- [ ] Write `deepEqual(a, b)` that correctly compares nested objects.
- [ ] Convert an array of `{ key, value }` pairs into a Map, then back to a plain object.
- [ ] Refactor a component that mutates `this.state` directly to use immutable updates.

---

> **Module 4 complete.** Type `continue` to proceed to **Module 5 – Scope, Hoisting & Closures.**

---

# Module 5 – Scope, Hoisting & Closures

---

## 5.1 What is Scope?

### Simple Explanation
Scope determines **where a variable is accessible** in your code.

### Deep Explanation
JavaScript has three types of scope:

1. **Global Scope** — variables declared outside any function or block; accessible everywhere.
2. **Function Scope** — variables declared inside a function; only accessible inside that function.
3. **Block Scope** — variables declared with `let`/`const` inside `{}`; only accessible within that block.

### Mental Model
> Scope is like a **one-way mirror**. Inner scopes can see out to outer scopes, but outer scopes cannot see in.

```
┌─────────────────────────────────────────┐
│  Global Scope                           │
│  let globalVar = 'I am global';         │
│  ┌───────────────────────────────────┐  │
│  │  Function Scope                   │  │
│  │  let fnVar = 'I am in function';  │  │
│  │  ┌───────────────────────────┐    │  │
│  │  │  Block Scope              │    │  │
│  │  │  let blockVar = 'block';  │    │  │
│  │  │  // can see fnVar ✅      │    │  │
│  │  │  // can see globalVar ✅  │    │  │
│  │  └───────────────────────────┘    │  │
│  │  // cannot see blockVar ❌        │  │
│  └───────────────────────────────────┘  │
│  // cannot see fnVar ❌                 │
└─────────────────────────────────────────┘
```

---

## 5.2 Scope Chain

### How Variable Lookup Works
When JavaScript encounters a variable, it searches for it in the **current scope first**, then moves up to the enclosing scope, continuing until it reaches the global scope. If not found anywhere — `ReferenceError`.

```js
const globalVal = 'global';

function outer() {
  const outerVal = 'outer';

  function inner() {
    const innerVal = 'inner';
    console.log(innerVal);   // ✅ found in own scope
    console.log(outerVal);   // ✅ found in outer scope
    console.log(globalVal);  // ✅ found in global scope
    console.log(unknown);    // ❌ ReferenceError
  }

  inner();
  console.log(innerVal); // ❌ ReferenceError — inner is not accessible here
}
```

### Shadowing
```js
const name = 'Global Alice';

function greet() {
  const name = 'Local Bob';  // shadows the outer name
  console.log(name);          // 'Local Bob'
}

greet();
console.log(name); // 'Global Alice' — outer unaffected
```

---

## 5.3 Hoisting — Complete Picture

> Covered briefly in Module 1 & 2. Here is the definitive deep dive.

### What Gets Hoisted and How

```js
// ── BEFORE execution, JS engine processes the entire scope ──

// 1. Function declarations — fully hoisted (name + body)
hello();                         // ✅ 'Hello!'
function hello() { console.log('Hello!'); }

// 2. var — declaration hoisted, initialization NOT
console.log(x);                  // undefined (not ReferenceError)
var x = 5;
console.log(x);                  // 5

// 3. let / const — hoisted but in TDZ until initialization
console.log(y);                  // ❌ ReferenceError (TDZ)
let y = 10;

// 4. Function expressions — depends on declaration keyword
greet();                         // ❌ TypeError: greet is not a function
var greet = function() {};        // var hoisted as undefined; undefined() = TypeError

greet2();                        // ❌ ReferenceError (TDZ)
let greet2 = function() {};

// 5. Class declarations — hoisted but TDZ (like let)
const p = new Person();          // ❌ ReferenceError
class Person {}
```

### The Hoisting Mental Model
```
Creation Phase (before any code runs):
  1. Register all function declarations — load full body
  2. Register all var declarations — set to undefined
  3. Register all let/const/class — place in TDZ

Execution Phase (code runs line by line):
  4. Assignments happen
  5. TDZ lifts when let/const line is reached
```

### Classic Interview Trap
```js
var x = 'global';

function test() {
  console.log(x);  // undefined — NOT 'global'
  var x = 'local';
  console.log(x);  // 'local'
}
test();
// Explanation: var x inside test() is hoisted to top of test(),
// shadowing global x. At first console.log, x exists but is undefined.
```

---

## 5.4 Closures

### Simple Explanation
A **closure** is a function that **remembers the variables from its outer scope** even after that outer scope has finished executing.

### Deep Explanation
Every function in JavaScript forms a closure over its **lexical environment** — the scope chain at the time of its creation. The inner function keeps a **live reference** to outer variables, not a copy.

### Mental Model
> A closure is a **backpack**. When a function is created, it packs the variables it needs from its outer scope into that backpack and carries them wherever it goes.

### Basic Closure
```js
function makeCounter() {
  let count = 0;           // outer variable

  return function() {      // inner function — closes over count
    count++;
    return count;
  };
}

const counter = makeCounter();
counter(); // 1
counter(); // 2
counter(); // 3

// count is NOT accessible from outside
console.log(count); // ReferenceError
```

### Closures Remember Live References
```js
function makeAdder(x) {
  return (y) => x + y;   // closes over x
}

const add5  = makeAdder(5);
const add10 = makeAdder(10);

add5(3);   // 8
add10(3);  // 13
// Each closure has its own x — independent state
```

### Closure Over Loop — The Classic Bug
```js
// ❌ BUG — var shares one binding across all iterations
for (var i = 0; i < 3; i++) {
  setTimeout(() => console.log(i), 100);
}
// Prints: 3, 3, 3
// All three callbacks close over the SAME i, which ends at 3

// ✅ FIX 1 — use let (block-scoped: new i per iteration)
for (let i = 0; i < 3; i++) {
  setTimeout(() => console.log(i), 100);
}
// Prints: 0, 1, 2

// ✅ FIX 2 — IIFE to capture current value
for (var i = 0; i < 3; i++) {
  (function(j) {
    setTimeout(() => console.log(j), 100);
  })(i);
}
// Prints: 0, 1, 2
```

---

## 5.5 Practical Closure Patterns

### Module Pattern (Pre-ES6 Modules)
```js
const counterModule = (function() {
  let count = 0;  // private state

  return {
    increment() { count++; },
    decrement() { count--; },
    getCount()  { return count; }
  };
})();

counterModule.increment();
counterModule.increment();
counterModule.getCount(); // 2
counterModule.count;      // undefined — truly private
```

### Memoization
```js
function memoize(fn) {
  const cache = new Map();  // closed over

  return function(...args) {
    const key = JSON.stringify(args);
    if (cache.has(key)) {
      console.log('cache hit');
      return cache.get(key);
    }
    const result = fn(...args);
    cache.set(key, result);
    return result;
  };
}

const expensiveCalc = memoize((n) => {
  // simulate expensive computation
  return n * n;
});

expensiveCalc(10); // computed: 100
expensiveCalc(10); // cache hit: 100
```

### Partial Application & Currying
```js
// Partial application — fix some arguments
function multiply(a, b) { return a * b; }

function partial(fn, ...presetArgs) {
  return (...laterArgs) => fn(...presetArgs, ...laterArgs);
}

const double = partial(multiply, 2);
const triple = partial(multiply, 3);
double(5);  // 10
triple(5);  // 15

// Currying — one argument at a time
const curry = fn => {
  return function curried(...args) {
    if (args.length >= fn.length) {
      return fn(...args);
    }
    return (...more) => curried(...args, ...more);
  };
};

const add = curry((a, b, c) => a + b + c);
add(1)(2)(3); // 6
add(1, 2)(3); // 6
add(1)(2, 3); // 6
```

### Factory with Private State
```js
function createUser(name, role) {
  // private
  let loginCount = 0;
  const createdAt = Date.now();

  // public API
  return {
    getName:      ()  => name,
    getRole:      ()  => role,
    login:        ()  => { loginCount++; return loginCount; },
    getLoginCount: () => loginCount,
    getAge:       ()  => Date.now() - createdAt
  };
}

const alice = createUser('Alice', 'admin');
alice.login();            // 1
alice.login();            // 2
alice.getLoginCount();    // 2
alice.loginCount;         // undefined — private!
```

---

## 5.6 Lexical vs Dynamic Scope

JavaScript uses **lexical (static) scope** — scope is determined by where functions are **written** in the source code, not where they are **called**.

```js
const x = 'global';

function outer() {
  const x = 'outer';
  inner();          // inner is called here
}

function inner() {
  console.log(x);   // 'global' — not 'outer'!
}
// inner's scope chain is determined at DEFINITION time:
// inner → global. It has no reference to outer's scope.

outer(); // 'global'
```

> **`this`** is the exception — it uses dynamic binding (determined at call time). Arrow functions restore lexical behavior for `this`.

---

## 5.7 Closure Memory & Garbage Collection

### How Closures Affect Memory
```js
function outer() {
  const largeArray = new Array(1_000_000).fill('data'); // 1M items

  return function inner() {
    return largeArray[0]; // closes over largeArray
  };
}

const fn = outer();
// largeArray is NOT garbage collected — fn holds a reference to it
fn(); // 'data'
fn = null; // now largeArray CAN be garbage collected
```

### Memory Leak Pattern to Avoid
```js
// ❌ Accumulating closures in a loop
const listeners = [];
for (let i = 0; i < 1000; i++) {
  listeners.push(() => console.log(i)); // 1000 closures, each holding i
}
// Fix: avoid creating closures in tight loops when not needed
```

### Angular Relevance
```ts
// Unsubscribed observables keep closure alive = memory leak
ngOnInit() {
  // ❌ Never unsubscribed
  this.service.data$.subscribe(data => {
    this.data = data; // closure over 'this'
  });
}

// ✅ Unsubscribe on destroy
private destroy$ = new Subject<void>();

ngOnInit() {
  this.service.data$
    .pipe(takeUntil(this.destroy$))
    .subscribe(data => { this.data = data; });
}

ngOnDestroy() {
  this.destroy$.next();
  this.destroy$.complete();
}
```

---

## 5.8 IIFE & Scope Isolation

```js
// IIFE creates a new scope — prevents polluting global
(function() {
  var privateVar = 'safe';
  // ... code ...
})();
console.log(typeof privateVar); // 'undefined'

// Block scope alternative (modern)
{
  let privateVar = 'safe';
  // ... code ...
}
console.log(typeof privateVar); // 'undefined'
```

---

## 5.9 Scope in Classes

```js
class BankAccount {
  // Public class field
  owner;

  // Private class field (ES2022 — true privacy, not closure)
  #balance = 0;

  constructor(owner, initialBalance) {
    this.owner = owner;
    this.#balance = initialBalance;
  }

  deposit(amount) {
    if (amount > 0) this.#balance += amount;
  }

  get balance() {
    return this.#balance;
  }
}

const account = new BankAccount('Alice', 1000);
account.deposit(500);
account.balance;    // 1500
account.#balance;   // ❌ SyntaxError — truly private
```

### Angular Relevance
```ts
@Component({ ... })
export class UserComponent {
  // Public — accessible in template
  users: User[] = [];

  // Private — internal only
  #cache = new Map<number, User>();

  // Protected — accessible in subclasses
  protected config = defaultConfig;
}
```

---

## 5.10 Common Mistakes & Edge Cases

```js
// 1. Accidental global variable
function setName() {
  name = 'Alice'; // no let/const/var — becomes global!
}
// Use strict mode to prevent: 'use strict';

// 2. Closure shares mutable reference
function makeMultipliers() {
  const result = [];
  for (var i = 1; i <= 3; i++) {
    result.push(() => i * 10);  // all share same i
  }
  return result;
}
makeMultipliers().map(fn => fn()); // [40, 40, 40] ← all see i=4
// Fix: use let in the for loop

// 3. Re-assigning outer variable through closure side-effect
function makeToggle() {
  let on = false;
  return () => (on = !on);
}
const toggle = makeToggle();
toggle(); // true
toggle(); // false
toggle(); // true

// 4. Stale closure (common in React hooks, less so Angular)
let count = 0;
const logCount = () => console.log(count); // captures count = 0
count = 5;
logCount(); // 5 — not stale here because count is in outer scope
// Stale closures happen when you capture a COPY of a primitive:
function staleExample() {
  let n = 0;
  const log = () => console.log(n);
  n = 10;
  log(); // 10 — JS closures capture by reference, not value
}
```

---

## Module 5 — Summary Notes

| Concept              | Key Takeaway                                                                   |
|----------------------|-------------------------------------------------------------------------------|
| Scope types          | Global → Function → Block (let/const only)                                   |
| Scope chain          | Inner can see outer; outer cannot see inner                                   |
| Lexical scope        | Scope determined at write time, not call time                                 |
| Hoisting             | `var` → `undefined`; `let/const/class` → TDZ; fn declarations → full body   |
| Closure              | Function remembers its outer scope variables — live reference, not a copy    |
| Loop + var bug       | All iterations share one `var`; fix with `let` or IIFE                       |
| Module pattern       | IIFE + closure = private state before ES modules                             |
| Memoization          | Cache results in closure — powerful optimization pattern                      |
| Currying/partial     | Closures enable deferred argument application                                 |
| Memory               | Closures keep outer vars alive — unsubscribe in Angular to prevent leaks     |
| Private class fields | `#field` — true privacy at engine level, not just convention                 |

---

## Module 5 — Checkpoint Tasks

- [ ] Predict the output of 5 different closure + loop combinations.
- [ ] Implement `memoize(fn)` that handles multiple arguments correctly.
- [ ] Write a `createStore(initialState)` factory using closures — with `getState()`, `setState()`, and `subscribe()` methods.
- [ ] Implement a full `curry(fn)` that supports partial application at any step.
- [ ] Identify and fix a memory leak in an Angular component using closures and subscriptions.
- [ ] Explain step-by-step why this prints what it does:
  ```js
  for (var i = 0; i < 3; i++) {
    setTimeout(function() { console.log(i); }, i * 1000);
  }
  ```

---

> **Module 5 complete.** Type `continue` to proceed to **Module 6 – Async JavaScript.**

---

# Module 6 – Async JavaScript

---

## 6.1 Why Async?

### Simple Explanation
JavaScript is **single-threaded** — it can only do one thing at a time. Async programming allows long-running operations (network requests, timers, file reads) to run without blocking the main thread.

### Deep Explanation
If JavaScript were purely synchronous:
- A network request taking 2 seconds would **freeze the UI** for 2 seconds.
- No user interaction, no animations, no rendering — completely blocked.

Async solves this via the **Event Loop**, which allows JS to delegate waiting tasks and continue executing other code.

### The Concurrency Model
```
┌─────────────────────────────────────────────────────┐
│                   JS Engine                         │
│  ┌─────────────┐    ┌──────────────────────────┐   │
│  │  Call Stack │    │        Heap               │   │
│  │             │    │  (object memory)          │   │
│  │  fn3()      │    └──────────────────────────┘   │
│  │  fn2()      │                                    │
│  │  fn1()      │                                    │
│  └──────┬──────┘                                    │
│         │                                           │
└─────────┼───────────────────────────────────────────┘
          │
┌─────────▼───────────────────────────────────────────┐
│              Browser / Node APIs                    │
│  setTimeout │ fetch │ DOM events │ fs.readFile      │
└─────────────────────────┬───────────────────────────┘
                          │ callback/promise resolved
┌─────────────────────────▼───────────────────────────┐
│           Callback Queue / Microtask Queue          │
│  Macrotasks: setTimeout, setInterval, I/O           │
│  Microtasks: Promise.then, queueMicrotask, MutObs   │
└─────────────────────────┬───────────────────────────┘
                          │ Event Loop picks next task
                          │ when Call Stack is empty
                          ▼
                    Back to Call Stack
```

---

## 6.2 The Event Loop

### How It Works — Step by Step
```js
console.log('1 — sync');

setTimeout(() => console.log('2 — macrotask'), 0);

Promise.resolve().then(() => console.log('3 — microtask'));

console.log('4 — sync');

// Output:
// 1 — sync
// 4 — sync
// 3 — microtask    ← microtasks run before macrotasks
// 2 — macrotask
```

### Microtasks vs Macrotasks

| Type        | Examples                                      | Priority |
|-------------|-----------------------------------------------|----------|
| Microtask   | `Promise.then`, `queueMicrotask`, `MutationObserver` | Higher — runs after current task, before next macrotask |
| Macrotask   | `setTimeout`, `setInterval`, `I/O`, `requestAnimationFrame` | Lower — one per event loop tick |

### Mental Model
> The Event Loop is a **security guard at a door**. It only lets someone new in when the room (call stack) is completely empty. Microtasks are VIPs — they all get in before any regular macrotask.

```js
// Advanced: multiple microtasks all drain before macrotasks
setTimeout(() => console.log('timeout'), 0);

Promise.resolve()
  .then(() => { console.log('micro 1'); return Promise.resolve(); })
  .then(() => console.log('micro 2'))
  .then(() => console.log('micro 3'));

// Output: micro 1, micro 2, micro 3, timeout
```

---

## 6.3 Callbacks

### Simple Explanation
A **callback** is a function passed as an argument to another function, to be called when an async operation completes.

```js
// Basic callback pattern
function fetchData(url, onSuccess, onError) {
  // simulate async
  setTimeout(() => {
    if (url) onSuccess({ data: 'result' });
    else onError(new Error('No URL'));
  }, 1000);
}

fetchData(
  'https://api.example.com',
  (data) => console.log('Success:', data),
  (err)  => console.error('Error:', err)
);
```

### Callback Hell
```js
// ❌ Deeply nested callbacks — "Pyramid of Doom"
login(user, (err, session) => {
  if (err) return handleError(err);
  getProfile(session.id, (err, profile) => {
    if (err) return handleError(err);
    getPermissions(profile.role, (err, perms) => {
      if (err) return handleError(err);
      loadDashboard(perms, (err, dashboard) => {
        if (err) return handleError(err);
        render(dashboard);
      });
    });
  });
});
// Hard to read, error-prone, difficult to maintain
```

Callbacks alone lead to callback hell. **Promises** solve this.

---

## 6.4 Promises

### Simple Explanation
A **Promise** is an object representing the eventual completion (or failure) of an async operation.

### States
```
Pending → Fulfilled (resolved with value)
        → Rejected  (rejected with reason)
```
Once settled, a Promise **never changes state**.

### Creating a Promise
```js
const promise = new Promise((resolve, reject) => {
  // async operation
  setTimeout(() => {
    const success = true;
    if (success) resolve('Data fetched!');
    else reject(new Error('Fetch failed'));
  }, 1000);
});

promise
  .then(data  => console.log(data))   // 'Data fetched!'
  .catch(err  => console.error(err))
  .finally(()  => console.log('Done')); // always runs
```

### Promise Chaining (solving callback hell)
```js
// ✅ Flat, readable chain
login(user)
  .then(session   => getProfile(session.id))
  .then(profile   => getPermissions(profile.role))
  .then(perms     => loadDashboard(perms))
  .then(dashboard => render(dashboard))
  .catch(err      => handleError(err));          // catches ANY error in chain
```

### Rules of Chaining
```js
// Return from .then passes value to next .then
Promise.resolve(1)
  .then(n => n + 1)    // receives 1, returns 2
  .then(n => n * 3)    // receives 2, returns 6
  .then(n => console.log(n)); // 6

// Return a Promise — chain waits for it
fetch('/api/users')
  .then(response => response.json())   // returns a Promise
  .then(users    => console.log(users));
```

### Promise Static Methods

```js
// Promise.all — all must succeed; rejects if ANY fails
const [users, posts, comments] = await Promise.all([
  fetch('/api/users').then(r => r.json()),
  fetch('/api/posts').then(r => r.json()),
  fetch('/api/comments').then(r => r.json()),
]);
// All 3 run in PARALLEL — much faster than sequential

// Promise.allSettled — waits for all, regardless of failures
const results = await Promise.allSettled([
  Promise.resolve('ok'),
  Promise.reject('fail'),
  Promise.resolve('ok2'),
]);
results.forEach(r => {
  if (r.status === 'fulfilled') console.log(r.value);
  else console.error(r.reason);
});

// Promise.race — resolves/rejects with FIRST settled promise
const result = await Promise.race([
  fetch('/api/fast'),
  new Promise((_, reject) => setTimeout(() => reject(new Error('Timeout')), 3000))
]);

// Promise.any — resolves with FIRST fulfilled; rejects if ALL fail
const fastest = await Promise.any([
  fetch('/cdn1/resource'),
  fetch('/cdn2/resource'),
  fetch('/cdn3/resource'),
]);
```

---

## 6.5 Async / Await

### Simple Explanation
`async/await` is syntactic sugar over Promises — it makes async code look and behave like synchronous code.

### Syntax
```js
async function fetchUser(id) {
  try {
    const response = await fetch(`/api/users/${id}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const user = await response.json();
    return user;
  } catch (err) {
    console.error('Failed to fetch user:', err);
    throw err; // re-throw so caller can handle
  }
}
```

### Key Rules
1. `async` function always returns a Promise (even if you return a plain value).
2. `await` can only be used inside an `async` function (or top-level in modules).
3. `await` pauses execution of the **async function** only — not the entire thread.
4. Errors must be caught with `try/catch` or `.catch()` on the returned Promise.

### Sequential vs Parallel
```js
// ❌ Sequential — total time = sum of all waits
async function sequential() {
  const users    = await fetchUsers();    // wait 1s
  const posts    = await fetchPosts();    // wait 1s
  const comments = await fetchComments(); // wait 1s
  // Total: ~3 seconds
}

// ✅ Parallel — total time = longest single wait
async function parallel() {
  const [users, posts, comments] = await Promise.all([
    fetchUsers(),
    fetchPosts(),
    fetchComments(),
  ]);
  // Total: ~1 second
}

// ✅ Start all, then await each (same as Promise.all but more flexible)
async function parallel2() {
  const usersPromise    = fetchUsers();
  const postsPromise    = fetchPosts();
  const commentsPromise = fetchComments();

  const users    = await usersPromise;
  const posts    = await postsPromise;
  const comments = await commentsPromise;
}
```

### Async in Loops
```js
const ids = [1, 2, 3, 4, 5];

// ❌ forEach does NOT await — all fire simultaneously, no control
ids.forEach(async id => {
  const user = await fetchUser(id); // forEach ignores returned promises
  console.log(user);
});

// ✅ for...of — sequential (use when order or rate limiting matters)
for (const id of ids) {
  const user = await fetchUser(id);
  console.log(user);
}

// ✅ map + Promise.all — parallel (use when all independent)
const users = await Promise.all(ids.map(id => fetchUser(id)));
```

### Error Handling Patterns
```js
// Pattern 1 — try/catch (standard)
async function loadData() {
  try {
    const data = await fetchData();
    return data;
  } catch (err) {
    handleError(err);
  }
}

// Pattern 2 — catch on the Promise
const data = await fetchData().catch(err => {
  handleError(err);
  return null; // fallback value
});

// Pattern 3 — helper for [error, data] tuple (Go-style)
async function safe(promise) {
  try {
    return [null, await promise];
  } catch (err) {
    return [err, null];
  }
}

const [err, user] = await safe(fetchUser(1));
if (err) return handleError(err);
console.log(user);
```

---

## 6.6 Generators & Iterators (Foundation of async/await)

### Iterators
```js
// An iterator is any object with a next() method
const range = {
  from: 1,
  to: 5,
  [Symbol.iterator]() {
    let current = this.from;
    const last = this.to;
    return {
      next() {
        return current <= last
          ? { value: current++, done: false }
          : { value: undefined, done: true };
      }
    };
  }
};

for (const num of range) console.log(num); // 1,2,3,4,5
[...range]; // [1,2,3,4,5]
```

### Generators
```js
// Generator function — produces a sequence lazily
function* counter(start = 0) {
  while (true) {
    yield start++;  // pause here, return value, resume on next()
  }
}

const gen = counter(1);
gen.next(); // { value: 1, done: false }
gen.next(); // { value: 2, done: false }
gen.next(); // { value: 3, done: false }

// Finite generator
function* range(start, end, step = 1) {
  for (let i = start; i < end; i += step) {
    yield i;
  }
}

[...range(0, 10, 2)]; // [0, 2, 4, 6, 8]
```

### Angular Relevance
> Generators are the conceptual underpinning of `async/await` (which is essentially a generator that yields Promises). They also appear in **Redux-Saga** patterns and some advanced RxJS implementations.

---

## 6.7 Async Patterns in Angular

### HTTP Requests with Angular `HttpClient`
```ts
// Service
@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private http: HttpClient) {}

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>('/api/users');
  }

  // Using lastValueFrom to convert Observable → Promise
  async getUsersAsync(): Promise<User[]> {
    return lastValueFrom(this.http.get<User[]>('/api/users'));
  }
}
```

### Component with async/await
```ts
@Component({ ... })
export class UserComponent implements OnInit {
  users: User[] = [];
  loading = false;
  error: string | null = null;

  constructor(private userService: UserService) {}

  async ngOnInit() {
    this.loading = true;
    try {
      this.users = await this.userService.getUsersAsync();
    } catch (err) {
      this.error = 'Failed to load users';
    } finally {
      this.loading = false;
    }
  }
}
```

### Angular Async Pipe (Preferred Pattern)
```ts
// ✅ Template handles subscription/unsubscription automatically
@Component({
  template: `
    <ng-container *ngIf="users$ | async as users">
      <div *ngFor="let user of users">{{ user.name }}</div>
    </ng-container>
  `
})
export class UserComponent {
  users$ = this.userService.getUsers();  // Observable — no manual subscribe
  constructor(private userService: UserService) {}
}
```

### Loading/Error State Pattern
```ts
readonly vm$ = this.userService.getUsers().pipe(
  map(users  => ({ users, loading: false, error: null })),
  startWith(  { users: [],  loading: true,  error: null }),
  catchError(err => of({ users: [], loading: false, error: err.message }))
);
```

---

## 6.8 Common Mistakes & Edge Cases

```js
// 1. Forgetting await
async function getUser() {
  const user = fetchUser(1); // ❌ returns Promise, not User
  console.log(user.name);    // undefined
}

// 2. Unhandled Promise rejection
fetchUser(99); // ❌ no .catch or try/catch — silently fails in some envs
// Always handle: fetchUser(99).catch(console.error);

// 3. await in a non-async function
function load() {
  const data = await fetch('/api'); // ❌ SyntaxError
}

// 4. Promise constructor anti-pattern
// ❌ Wrapping an already-promisified function
function fetchUsers() {
  return new Promise((resolve, reject) => {
    fetch('/api/users')
      .then(r => r.json())
      .then(resolve)
      .catch(reject);
  });
}
// ✅ Just return the Promise directly
function fetchUsers() {
  return fetch('/api/users').then(r => r.json());
}

// 5. Error swallowing
async function load() {
  try {
    return await riskyOp();
  } catch (e) {
    // ❌ Silent — caller never knows it failed
  }
}
// ✅ Always re-throw or return a meaningful fallback
```

---

## Module 6 — Summary Notes

| Concept              | Key Takeaway                                                                    |
|----------------------|--------------------------------------------------------------------------------|
| Event Loop           | Call stack must be empty before callbacks/promises run                        |
| Microtasks           | Run before macrotasks — Promise.then, queueMicrotask                         |
| Callbacks            | Simple but lead to callback hell — replaced by Promises                      |
| Promise states       | Pending → Fulfilled / Rejected — immutable once settled                      |
| Promise chaining     | `.then()` returns a new Promise — enables flat, readable chains              |
| `Promise.all`        | Parallel execution — fails fast if any rejects                               |
| `Promise.allSettled` | Parallel — waits for all regardless of failure                               |
| `async/await`        | Syntactic sugar over Promises — synchronous-looking async code               |
| Sequential vs parallel | `await` in loop = sequential; `Promise.all` = parallel                    |
| Angular async pipe   | `| async` — auto-subscribe, auto-unsubscribe, change detection aware        |
| Error handling       | Always `try/catch` in async functions — never swallow errors silently        |

---

## Module 6 — Checkpoint Tasks

- [ ] Predict the exact output order of a mixed sync/Promise/setTimeout block.
- [ ] Convert a callback-based API (`fs.readFile`) into a Promise-based function.
- [ ] Write `fetchWithTimeout(url, ms)` that rejects if the request takes too long using `Promise.race`.
- [ ] Implement `retryAsync(fn, retries)` that retries a failing async function N times.
- [ ] Refactor a component that uses multiple sequential `await` calls into parallel `Promise.all`.
- [ ] Build a `queue(tasks, concurrency)` that runs async tasks with limited parallelism (e.g., max 3 at a time).

---

> **Module 6 complete.** Type `continue` to proceed to **Module 7 – Prototype & OOP.**

---

# Module 7 – Prototype & OOP

---

## 7.1 The Prototype System

### Simple Explanation
Every JavaScript object has a hidden link to another object called its **prototype**. When you access a property that doesn't exist on the object, JS automatically looks up the prototype chain.

### Deep Explanation
- Every object has an internal `[[Prototype]]` slot (accessible via `Object.getPrototypeOf()`).
- The chain ends at `Object.prototype`, whose `[[Prototype]]` is `null`.
- This is **prototypal inheritance** — objects inherit directly from other objects, not from classes.
- ES6 `class` syntax is 100% syntactic sugar — under the hood it still uses prototypes.

### Mental Model
> The prototype chain is like a **family tree lookup**. If you can't find a trait in yourself, you check your parent, then grandparent, until you reach the end of the family line.

```
myObj → MyConstructor.prototype → Object.prototype → null
```

### Visualising the Chain
```js
const arr = [1, 2, 3];

// arr has no push() of its own
// arr → Array.prototype (has push) → Object.prototype → null

Object.getPrototypeOf(arr) === Array.prototype;          // true
Object.getPrototypeOf(Array.prototype) === Object.prototype; // true
Object.getPrototypeOf(Object.prototype);                 // null

// Proof of delegation
arr.hasOwnProperty('push');          // false — not on arr itself
'push' in arr;                       // true  — found on Array.prototype
```

---

## 7.2 Constructor Functions & `new`

### What `new` Does — Step by Step
```js
function User(name, role) {
  this.name = name;
  this.role = role;
}
User.prototype.greet = function() {
  return `Hi, I'm ${this.name}`;
};

const alice = new User('Alice', 'admin');
```

When `new User(...)` is called:
1. A **new empty object** is created: `{}`
2. Its `[[Prototype]]` is set to `User.prototype`
3. `User` is called with `this` = that new object
4. The new object is **returned** (unless the constructor explicitly returns a different object)

```js
// Manual simulation of new
function myNew(Constructor, ...args) {
  const obj = Object.create(Constructor.prototype); // steps 1 & 2
  const result = Constructor.apply(obj, args);       // step 3
  return result instanceof Object ? result : obj;    // step 4
}

const bob = myNew(User, 'Bob', 'user');
bob.greet(); // 'Hi, I'm Bob'
```

---

## 7.3 ES6 Classes

### Syntax & Internals
```js
class Animal {
  // Class field (ES2022) — instance property without constructor
  type = 'animal';

  constructor(name) {
    this.name = name;
  }

  // Prototype method — shared across all instances
  speak() {
    return `${this.name} makes a sound`;
  }

  // Static method — on the class itself, not instances
  static create(name) {
    return new Animal(name);
  }

  // Getter
  get info() {
    return `${this.name} (${this.type})`;
  }

  // Setter
  set info(value) {
    this.name = value;
  }
}

const dog = new Animal('Rex');
dog.speak();              // 'Rex makes a sound'
dog.info;                 // 'Rex (animal)'
Animal.create('Cat');     // Animal { name: 'Cat' }

// Proof it's still prototype-based
dog.speak === Animal.prototype.speak; // true — shared, not copied
```

### Class vs Constructor Function
```js
// These are equivalent:
class User {
  constructor(name) { this.name = name; }
  greet() { return `Hi, ${this.name}`; }
}

function User(name) { this.name = name; }
User.prototype.greet = function() { return `Hi, ${this.name}`; };

// Differences:
// 1. class declarations are NOT callable without `new` (throws TypeError)
// 2. class body is always in strict mode
// 3. class declarations are TDZ (like let) — not hoisted
// 4. Methods defined with class are non-enumerable
```

---

## 7.4 Inheritance

### `extends` & `super`
```js
class Animal {
  constructor(name) {
    this.name = name;
  }
  speak() {
    return `${this.name} makes a sound`;
  }
  toString() {
    return `Animal: ${this.name}`;
  }
}

class Dog extends Animal {
  constructor(name, breed) {
    super(name);          // MUST call super() before using this
    this.breed = breed;
  }

  // Override parent method
  speak() {
    return `${this.name} barks`;
  }

  // Call parent method
  fullInfo() {
    return `${super.speak()} — breed: ${this.breed}`;
  }
}

const rex = new Dog('Rex', 'Labrador');
rex.speak();       // 'Rex barks'
rex.fullInfo();    // 'Rex makes a sound — breed: Labrador'
rex instanceof Dog;    // true
rex instanceof Animal; // true — chain works
```

### Prototype Chain After `extends`
```
rex → Dog.prototype → Animal.prototype → Object.prototype → null
```

### Multi-level Inheritance
```js
class LoudDog extends Dog {
  speak() {
    return super.speak().toUpperCase() + '!!!';
  }
}

new LoudDog('Max', 'Poodle').speak(); // 'MAX BARKS!!!'
```

---

## 7.5 Mixins (Composition over Inheritance)

### The Problem with Deep Inheritance
```
Vehicle → MotorVehicle → Car → ElectricCar → ElectricSportsCar
```
Deep hierarchies become rigid — a change at the top breaks everything below.

### Mixin Pattern
```js
// Mixins are plain objects or functions adding behaviour
const Serializable = (Base) => class extends Base {
  serialize() {
    return JSON.stringify(this);
  }
  static deserialize(json) {
    return Object.assign(new this(), JSON.parse(json));
  }
};

const Validatable = (Base) => class extends Base {
  validate() {
    return Object.values(this).every(v => v !== null && v !== undefined);
  }
};

const Timestamped = (Base) => class extends Base {
  constructor(...args) {
    super(...args);
    this.createdAt = new Date().toISOString();
  }
};

// Compose mixins
class BaseModel {
  constructor(data) { Object.assign(this, data); }
}

class User extends Timestamped(Validatable(Serializable(BaseModel))) {}

const user = new User({ name: 'Alice', role: 'admin' });
user.validate();   // true
user.serialize();  // '{"name":"Alice","role":"admin","createdAt":"..."}'
user.createdAt;    // ISO string
```

### Angular Relevance
Angular's own source code uses mixins for things like `HasValidator`, `ControlValueAccessor` combinations. Composition of behaviours is the Angular way.

---

## 7.6 `Object.create` — Prototypal Inheritance Directly

```js
// Create object with explicit prototype
const animalProto = {
  speak() { return `${this.name} makes a sound`; },
  toString() { return `[Animal: ${this.name}]`; }
};

const dog = Object.create(animalProto);
dog.name = 'Rex';
dog.speak(); // 'Rex makes a sound'

Object.getPrototypeOf(dog) === animalProto; // true

// Object.create(null) — no prototype at all (pure dictionary)
const dict = Object.create(null);
dict.key = 'value';
dict.toString;  // undefined — no Object.prototype methods!
// Use for safe hash maps that won't conflict with inherited property names
```

---

## 7.7 `instanceof` & Type Checking

```js
class Animal {}
class Dog extends Animal {}

const rex = new Dog();

rex instanceof Dog;     // true
rex instanceof Animal;  // true — inherits
rex instanceof Object;  // true — everything is

// instanceof checks the prototype chain:
// rex → Dog.prototype → Animal.prototype → Object.prototype

// Pitfall — instanceof fails across iframes/realms
[] instanceof Array;             // true (same realm)
// In iframes: [] instanceof window.parent.Array  // false!

// Safer checks
Array.isArray(rex);              // false
Object.prototype.toString.call(rex); // '[object Object]'
Object.prototype.toString.call([]); // '[object Array]'

// Custom instanceof behaviour with Symbol.hasInstance
class EvenNumber {
  static [Symbol.hasInstance](num) {
    return Number.isInteger(num) && num % 2 === 0;
  }
}
2 instanceof EvenNumber;  // true
3 instanceof EvenNumber;  // false
```

---

## 7.8 Private Fields & Encapsulation

```js
class BankAccount {
  // True private field — engine-enforced
  #balance;
  #owner;
  #transactions = [];

  constructor(owner, initialBalance) {
    this.#owner = owner;
    this.#balance = initialBalance;
  }

  deposit(amount) {
    if (amount <= 0) throw new Error('Amount must be positive');
    this.#balance += amount;
    this.#transactions.push({ type: 'deposit', amount, date: new Date() });
  }

  withdraw(amount) {
    if (amount > this.#balance) throw new Error('Insufficient funds');
    this.#balance -= amount;
    this.#transactions.push({ type: 'withdrawal', amount, date: new Date() });
  }

  get balance()       { return this.#balance; }
  get owner()         { return this.#owner; }
  get transactions()  { return [...this.#transactions]; } // defensive copy

  // Private method
  #formatCurrency(n) { return `$${n.toFixed(2)}`; }

  statement() {
    return this.#transactions
      .map(t => `${t.type}: ${this.#formatCurrency(t.amount)}`)
      .join('\n');
  }
}

const acc = new BankAccount('Alice', 1000);
acc.deposit(500);
acc.balance;       // 1500
acc.#balance;      // ❌ SyntaxError — truly private
```

---

## 7.9 SOLID Principles in JavaScript / Angular

### S — Single Responsibility
```ts
// ❌ Component doing too much
class UserComponent {
  fetchUsers() { /* ... */ }
  validateUser() { /* ... */ }
  formatDate() { /* ... */ }
  exportToCsv() { /* ... */ }
}

// ✅ Each class has one reason to change
class UserComponent { /* display only */ }
class UserService  { /* data fetching */ }
class UserValidator { /* validation */ }
class CsvExporter  { /* export logic */ }
```

### O — Open/Closed
```ts
// ❌ Modify class to add new behaviour
class Discount {
  calculate(type: string, price: number) {
    if (type === 'seasonal') return price * 0.9;
    if (type === 'vip') return price * 0.8;
    // Adding new type requires modifying this class
  }
}

// ✅ Open for extension, closed for modification
interface DiscountStrategy {
  calculate(price: number): number;
}
class SeasonalDiscount implements DiscountStrategy {
  calculate(price: number) { return price * 0.9; }
}
class VipDiscount implements DiscountStrategy {
  calculate(price: number) { return price * 0.8; }
}
```

### L — Liskov Substitution
> Derived classes must be substitutable for their base class without breaking behaviour.

### I — Interface Segregation
```ts
// ❌ Fat interface forces unneeded implementations
interface Worker {
  work(): void;
  eat(): void;
  sleep(): void;
}

// ✅ Split into focused interfaces
interface Workable  { work(): void; }
interface Feedable  { eat(): void; }
interface Restable  { sleep(): void; }
```

### D — Dependency Inversion
```ts
// ❌ High-level module depends on low-level detail
class OrderService {
  private db = new MySQLDatabase(); // tightly coupled
}

// ✅ Depend on abstraction (Angular DI does this naturally)
abstract class Database { abstract query(sql: string): any[]; }

@Injectable()
class OrderService {
  constructor(private db: Database) {} // injected abstraction
}
```

---

## 7.10 Angular OOP Patterns

### Base Component Pattern
```ts
// Abstract base with shared logic
abstract class BaseListComponent<T> implements OnInit, OnDestroy {
  items: T[] = [];
  loading = false;
  error: string | null = null;
  protected destroy$ = new Subject<void>();

  abstract loadItems(): Observable<T[]>;

  ngOnInit() {
    this.loading = true;
    this.loadItems()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next:  items => { this.items = items; this.loading = false; },
        error: err   => { this.error = err.message; this.loading = false; }
      });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}

// Concrete implementation
@Component({ template: `...` })
class UserListComponent extends BaseListComponent<User> {
  constructor(private userService: UserService) { super(); }
  loadItems() { return this.userService.getAll(); }
}
```

### Service with Abstract Interface
```ts
abstract class StorageService {
  abstract get<T>(key: string): T | null;
  abstract set<T>(key: string, value: T): void;
  abstract remove(key: string): void;
}

@Injectable()
class LocalStorageService extends StorageService {
  get<T>(key: string): T | null {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : null;
  }
  set<T>(key: string, value: T) {
    localStorage.setItem(key, JSON.stringify(value));
  }
  remove(key: string) { localStorage.removeItem(key); }
}
```

---

## Module 7 — Summary Notes

| Concept               | Key Takeaway                                                                   |
|-----------------------|-------------------------------------------------------------------------------|
| Prototype chain       | Property lookup walks chain until `null`; all objects link to `Object.prototype` |
| `new` keyword         | Creates object → sets prototype → calls constructor → returns object          |
| `class`               | Syntactic sugar over prototypes — methods go on `.prototype`, not instances   |
| `extends` / `super`   | Sets up prototype chain; `super()` required in subclass constructor           |
| Composition (Mixins)  | Prefer over deep inheritance — more flexible, testable, maintainable          |
| Private fields `#`    | Engine-enforced privacy — not just convention                                 |
| `instanceof`          | Checks prototype chain — can fail across realms                               |
| SOLID principles      | Angular's DI, components, and services are built around these                 |
| Abstract base class   | Share lifecycle and state logic across similar components                     |

---

## Module 7 — Checkpoint Tasks

- [ ] Implement `new` from scratch — write `myNew(Fn, ...args)`.
- [ ] Build a `Shape` class hierarchy: `Shape → Circle / Rectangle / Triangle`, each with `area()` and `perimeter()`.
- [ ] Implement the Mixin pattern to add `Serializable` and `Timestamped` behaviour to multiple unrelated classes.
- [ ] Refactor a monolithic service class into SOLID-compliant smaller units.
- [ ] Write an abstract `BaseFormComponent` that handles dirty/pristine state and validation for any form component that extends it.
- [ ] Explain the prototype chain of a Dog instance extending Animal — draw it out.

---

> **Module 7 complete.** Type `continue` to proceed to **Module 8 – DOM & Browser APIs.**

---

# Module 8 – DOM & Browser APIs

---

## 8.1 What is the DOM?

### Simple Explanation
The **Document Object Model (DOM)** is a tree-shaped, in-memory representation of an HTML document. JavaScript can read and manipulate this tree to change what the user sees.

### Deep Explanation
- The DOM is a **live** representation — changes reflect immediately in the browser.
- Every HTML element becomes a **Node** in the tree.
- The DOM is not part of JavaScript — it's a **browser API** (Web API). Node.js has no DOM.
- Angular **abstracts away** direct DOM manipulation, but understanding the DOM is essential to understand Angular's renderer, `ElementRef`, `Renderer2`, and `@ViewChild`.

### The DOM Tree
```
document
└── html
    ├── head
    │   ├── title
    │   └── meta
    └── body
        ├── header
        │   └── h1 ── "My App"
        ├── main
        │   ├── section
        │   │   └── p ── "Hello"
        │   └── ul
        │       ├── li ── "Item 1"
        │       └── li ── "Item 2"
        └── footer
```

---

## 8.2 Selecting Elements

```js
// By ID — returns single element or null
const header = document.getElementById('main-header');

// By CSS selector — returns FIRST match
const btn = document.querySelector('.submit-btn');
const input = document.querySelector('input[type="email"]');
const first = document.querySelector('ul > li:first-child');

// By CSS selector — returns ALL matches (NodeList, not array)
const items = document.querySelectorAll('.list-item');
const allBtns = document.querySelectorAll('button');

// Convert NodeList to Array for array methods
Array.from(items).filter(el => el.classList.contains('active'));
[...allBtns].map(btn => btn.textContent);

// Legacy selectors (still common in older codebases)
document.getElementsByClassName('card'); // HTMLCollection (live)
document.getElementsByTagName('div');    // HTMLCollection (live)

// Relative selectors
element.querySelector('.child');         // only within element's subtree
element.closest('.parent-class');        // walk UP the tree
element.parentElement;
element.children;                        // HTMLCollection of direct children
element.nextElementSibling;
element.previousElementSibling;
```

### Live vs Static Collections
```js
// HTMLCollection (live) — updates automatically when DOM changes
const liveList = document.getElementsByClassName('item');

// NodeList from querySelectorAll (static) — snapshot at query time
const staticList = document.querySelectorAll('.item');

document.body.appendChild(document.createElement('div')).className = 'item';
liveList.length;   // increased by 1
staticList.length; // unchanged
```

---

## 8.3 Manipulating Elements

### Content
```js
const el = document.querySelector('#output');

// Text content — safe, no HTML parsing
el.textContent = 'Hello World';

// Inner HTML — parses HTML, potential XSS risk
el.innerHTML = '<strong>Bold Text</strong>';

// ⚠️ NEVER inject unsanitised user input with innerHTML
el.innerHTML = userInput; // ❌ XSS vulnerability!

// Safe approach for user content
el.textContent = userInput; // ✅ always safe
```

### Attributes
```js
el.getAttribute('data-id');               // '42'
el.setAttribute('data-id', '99');
el.removeAttribute('disabled');
el.hasAttribute('hidden');                // true/false

// Boolean attributes
el.disabled = true;
el.hidden = false;

// Data attributes
el.dataset.userId;                        // reads data-user-id
el.dataset.userId = '42';                // sets data-user-id="42"
```

### CSS Classes
```js
el.classList.add('active', 'highlight');
el.classList.remove('loading');
el.classList.toggle('open');              // adds if absent, removes if present
el.classList.toggle('open', condition);   // force add/remove based on boolean
el.classList.contains('active');          // true/false
el.classList.replace('old-class', 'new-class');

// className — full string replacement
el.className = 'card active featured';   // replaces all classes
```

### Styles
```js
// Inline styles — use sparingly (prefer classes)
el.style.backgroundColor = '#007bff';
el.style.display = 'none';
el.style.cssText = 'color: red; font-size: 16px;';

// Computed styles (read-only — final rendered value)
const styles = window.getComputedStyle(el);
styles.getPropertyValue('font-size');    // '16px'
styles.getPropertyValue('--my-var');     // CSS custom property
```

---

## 8.4 Creating & Inserting Elements

```js
// Create
const div = document.createElement('div');
div.className = 'card';
div.textContent = 'New Card';

// Insert — modern methods (prefer these)
parent.append(child);                    // end of parent (accepts strings too)
parent.prepend(child);                   // start of parent
el.before(newEl);                        // before el in parent
el.after(newEl);                         // after el in parent
el.replaceWith(newEl);                   // replace el with newEl

// Insert adjacent HTML
el.insertAdjacentHTML('beforeend', '<span>text</span>');
// positions: 'beforebegin' | 'afterbegin' | 'beforeend' | 'afterend'

// Remove
el.remove();

// Clone
const clone = el.cloneNode(true);  // true = deep clone (includes children)

// Document Fragment — batch DOM operations for performance
const fragment = document.createDocumentFragment();
items.forEach(item => {
  const li = document.createElement('li');
  li.textContent = item;
  fragment.appendChild(li);
});
ul.appendChild(fragment); // single reflow instead of N reflows
```

---

## 8.5 Events

### Adding & Removing Event Listeners
```js
function handleClick(event) {
  console.log('clicked', event.target);
}

el.addEventListener('click', handleClick);
el.removeEventListener('click', handleClick);  // must be same function reference!

// Options
el.addEventListener('click', handler, { once: true });    // auto-removes after first call
el.addEventListener('scroll', handler, { passive: true }); // won't call preventDefault
el.addEventListener('click', handler, { capture: true });  // capture phase
```

### The Event Object
```js
el.addEventListener('click', (event) => {
  event.target;           // element that was clicked
  event.currentTarget;    // element the listener is on
  event.type;             // 'click'
  event.preventDefault(); // prevent default browser action (e.g., link navigation)
  event.stopPropagation(); // stop bubbling to parent elements
  event.stopImmediatePropagation(); // stop all remaining listeners on this element
  event.clientX;          // mouse X relative to viewport
  event.clientY;          // mouse Y relative to viewport
  event.key;              // keyboard key name
  event.code;             // physical key code
});
```

### Event Propagation — Bubbling & Capturing
```
Capture Phase (top → down):  document → html → body → div → button
Target Phase:                 button (event fires)
Bubble Phase (bottom → up):  button → div → body → html → document
```

```js
// Most events bubble — handlers on parents receive child events
document.body.addEventListener('click', (e) => {
  console.log('body received click from:', e.target.tagName);
});

// stopPropagation prevents bubbling
button.addEventListener('click', (e) => {
  e.stopPropagation(); // body click handler won't fire
});
```

### Event Delegation — Critical Pattern
Instead of attaching N listeners (one per item), attach ONE to the parent:

```js
// ❌ Inefficient — N listeners, breaks for dynamically added items
document.querySelectorAll('.list-item').forEach(item => {
  item.addEventListener('click', handleItemClick);
});

// ✅ Event delegation — one listener handles all current and future items
document.querySelector('.list').addEventListener('click', (event) => {
  const item = event.target.closest('.list-item');
  if (!item) return;  // click wasn't on a list-item
  handleItemClick(item);
});
```

### Custom Events
```js
// Create and dispatch
const event = new CustomEvent('user-selected', {
  bubbles: true,
  detail: { userId: 42, name: 'Alice' }
});
button.dispatchEvent(event);

// Listen
document.addEventListener('user-selected', (e) => {
  console.log(e.detail.userId); // 42
});
```

### Angular Relevance
```ts
// Angular's event binding compiles to addEventListener under the hood
// (click)="handler($event)" === el.addEventListener('click', handler)

// Angular EventEmitter is the equivalent of custom events
@Output() userSelected = new EventEmitter<User>();
this.userSelected.emit(user);

// Host listener — react to DOM events on the host element
@HostListener('click', ['$event'])
onClick(event: MouseEvent) { ... }

@HostListener('window:scroll', ['$event'])
onScroll(event: Event) { ... }
```

---

## 8.6 The Browser Environment

### `window` Object
```js
window.innerWidth;              // viewport width
window.innerHeight;             // viewport height
window.location.href;           // current URL
window.location.pathname;       // just the path
window.location.search;         // query string
window.location.hash;
window.history.pushState({}, '', '/new-path');
window.history.back();
window.scrollTo({ top: 0, behavior: 'smooth' });
window.open('https://example.com', '_blank');
window.alert('Message');        // blocking dialog (avoid in production)
window.confirm('Sure?');        // returns boolean
```

### `navigator` Object
```js
navigator.userAgent;            // browser identification string
navigator.language;             // 'en-US'
navigator.onLine;               // connectivity state
navigator.clipboard.writeText('copied!');
navigator.geolocation.getCurrentPosition(pos => {
  console.log(pos.coords.latitude, pos.coords.longitude);
});
```

---

## 8.7 Timers

```js
// setTimeout — run once after delay
const timerId = setTimeout(() => console.log('after 2s'), 2000);
clearTimeout(timerId);   // cancel before it fires

// setInterval — run repeatedly
const intervalId = setInterval(() => console.log('every 1s'), 1000);
clearInterval(intervalId); // ALWAYS clear to prevent memory leaks

// requestAnimationFrame — run before next repaint (~60fps)
function animate() {
  updatePosition();
  requestAnimationFrame(animate); // schedule next frame
}
requestAnimationFrame(animate);

// queueMicrotask — schedule a microtask
queueMicrotask(() => console.log('microtask'));
```

### Angular Relevance
```ts
// NgZone — run timers inside Angular's zone for change detection
constructor(private ngZone: NgZone) {}

startPolling() {
  this.ngZone.runOutsideAngular(() => {
    // ✅ Timer runs outside zone — no unnecessary change detection
    setInterval(() => {
      this.ngZone.run(() => {
        this.updateData(); // re-enter zone only when updating state
      });
    }, 5000);
  });
}
```

---

## 8.8 Storage APIs

```js
// localStorage — persists across sessions
localStorage.setItem('token', JSON.stringify(token));
const token = JSON.parse(localStorage.getItem('token') ?? 'null');
localStorage.removeItem('token');
localStorage.clear();

// sessionStorage — cleared when tab closes
sessionStorage.setItem('draft', JSON.stringify(draft));

// IndexedDB — large structured data (async, more complex)
// Use libraries like idb for a Promise-based API

// Cookies — sent with every HTTP request
document.cookie = 'theme=dark; path=/; SameSite=Strict';
```

### Security Notes
- Never store JWT tokens or sensitive data in `localStorage` — vulnerable to XSS.
- Prefer `httpOnly` cookies for auth tokens (inaccessible to JavaScript).
- Always sanitize data read from storage before using it.

---

## 8.9 Observers — MutationObserver, IntersectionObserver, ResizeObserver

### MutationObserver — watch DOM changes
```js
const observer = new MutationObserver((mutations) => {
  mutations.forEach(mutation => {
    console.log('Changed:', mutation.type, mutation.target);
  });
});

observer.observe(document.getElementById('container'), {
  childList: true,   // watch for added/removed child nodes
  subtree: true,     // observe all descendants
  attributes: true,  // watch attribute changes
  characterData: true // watch text content changes
});

observer.disconnect(); // stop observing
```

### IntersectionObserver — lazy load, infinite scroll
```js
const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('visible');
      // load image, trigger animation, fetch next page
      observer.unobserve(entry.target); // stop if one-time action
    }
  });
}, {
  threshold: 0.1,      // 10% visible triggers callback
  rootMargin: '0px 0px -50px 0px'  // shrink intersection area
});

document.querySelectorAll('.lazy').forEach(el => observer.observe(el));
```

### ResizeObserver — respond to element size changes
```js
const observer = new ResizeObserver((entries) => {
  for (const entry of entries) {
    const { width, height } = entry.contentRect;
    console.log(`Element resized: ${width}x${height}`);
  }
});
observer.observe(document.querySelector('.chart-container'));
```

### Angular Relevance
```ts
// Angular CDK provides ObserversModule wrapping MutationObserver
// IntersectionObserver used for virtual scrolling (@angular/cdk/scrolling)

@Directive({ selector: '[appLazyLoad]' })
export class LazyLoadDirective implements OnInit, OnDestroy {
  private observer!: IntersectionObserver;

  constructor(private el: ElementRef) {}

  ngOnInit() {
    this.observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        this.loadContent();
        this.observer.disconnect();
      }
    });
    this.observer.observe(this.el.nativeElement);
  }

  ngOnDestroy() { this.observer.disconnect(); }

  private loadContent() { /* ... */ }
}
```

---

## 8.10 Angular & DOM — The Right Way

Angular intentionally abstracts the DOM for server-side rendering (SSR), Web Workers, and testability. **Avoid direct DOM access** wherever possible.

### `ElementRef` — direct but discouraged
```ts
@Component({ ... })
export class MyComponent implements AfterViewInit {
  @ViewChild('myEl') myEl!: ElementRef<HTMLElement>;

  ngAfterViewInit() {
    // ⚠️ Direct DOM — breaks SSR, bypasses sanitization
    this.myEl.nativeElement.focus();
  }
}
```

### `Renderer2` — safe abstraction (preferred)
```ts
@Component({ ... })
export class MyComponent {
  constructor(
    private renderer: Renderer2,
    private el: ElementRef
  ) {}

  highlight() {
    // ✅ Works in SSR, Web Workers, proper sanitization
    this.renderer.setStyle(this.el.nativeElement, 'background', 'yellow');
    this.renderer.addClass(this.el.nativeElement, 'active');
    this.renderer.setAttribute(this.el.nativeElement, 'aria-selected', 'true');
  }
}
```

### Template Bindings — best approach
```html
<!-- ✅ Angular template — no direct DOM needed -->
<div
  [class.active]="isActive"
  [style.background]="bgColor"
  [attr.aria-label]="ariaLabel"
  (click)="onClick($event)">
</div>
```

---

## Module 8 — Summary Notes

| Concept               | Key Takeaway                                                                      |
|-----------------------|----------------------------------------------------------------------------------|
| DOM                   | Live tree representation of HTML — browser API, not JavaScript                  |
| `querySelector`       | Returns first match (static); `querySelectorAll` returns static NodeList        |
| Event delegation      | One parent listener handles all child events — efficient + works dynamically    |
| Event bubbling        | Events bubble up by default; `stopPropagation()` halts it                       |
| `innerHTML` danger    | Never inject unsanitized user input — use `textContent` instead                 |
| DocumentFragment      | Batch DOM insertions for a single reflow                                         |
| localStorage          | Never store sensitive tokens — vulnerable to XSS                                |
| Observers             | `MutationObserver`, `IntersectionObserver`, `ResizeObserver` — performant async DOM watching |
| Angular `Renderer2`   | Preferred over `ElementRef` for DOM manipulation — SSR-safe                     |
| Angular template      | Use `[class]`, `[style]`, `(event)` bindings — avoid raw DOM when possible      |

---

## Module 8 — Checkpoint Tasks

- [ ] Implement a `lazyLoad` directive using `IntersectionObserver` that loads an image only when scrolled into view.
- [ ] Build an event-delegated todo list — one listener on `<ul>` handles clicks on any `<li>`, including dynamically added ones.
- [ ] Write a safe `setHTML(element, userContent)` utility that prevents XSS.
- [ ] Use `MutationObserver` to detect when a specific element is added to the DOM.
- [ ] Build a `ResizeObserver`-powered responsive component that switches layout at certain widths.
- [ ] Refactor a component using `ElementRef.nativeElement` manipulation to use `Renderer2` instead.

---

> **Module 8 complete.** Type `continue` to proceed to **Module 9 – Modules & Modern ES6+ JavaScript.**

---

# Module 9 – Modules & Modern ES6+ JavaScript

---

## 9.1 Why Modules?

### Simple Explanation
Modules let you split code into separate files, each with its own scope. You explicitly `export` what you want to share and `import` what you need.

### Deep Explanation
Before ES Modules (ESM), JavaScript had no native module system. Developers used patterns like IIFEs and namespaces, then tools like CommonJS (Node.js) and AMD (RequireJS). ES2015 introduced the native `import/export` syntax which is now the standard — and what Angular is built on.

**Benefits of modules:**
- **Encapsulation** — private scope by default; only exports are public.
- **Explicit dependencies** — `import` makes dependencies visible and traceable.
- **Tree-shaking** — bundlers (Webpack, esbuild) can eliminate unused exports.
- **Lazy loading** — load code on demand, not upfront.

---

## 9.2 ES Module Syntax

### Named Exports
```js
// math.js — multiple named exports
export const PI = 3.14159;

export function add(a, b) { return a + b; }
export function subtract(a, b) { return a - b; }

export class Vector {
  constructor(x, y) { this.x = x; this.y = y; }
}

// Export at the end (preferred style — shows all exports in one place)
const multiply = (a, b) => a * b;
const divide   = (a, b) => a / b;
export { multiply, divide };

// Rename on export
export { multiply as mul, divide as div };
```

### Named Imports
```js
// Import specific names
import { add, subtract } from './math.js';

// Rename on import
import { add as sum } from './math.js';

// Import all as namespace
import * as Math from './math.js';
Math.add(2, 3);   // 5
Math.PI;          // 3.14159
```

### Default Export
```js
// user.js — one default export per file
export default class User {
  constructor(name) { this.name = name; }
}

// Or:
const config = { api: '/api', timeout: 3000 };
export default config;
```

### Default Import
```js
// Any name for default import
import User from './user.js';
import AppConfig from './config.js';

// Combine default and named
import User, { USER_ROLES, createUser } from './user.js';
```

### Re-exporting (Barrel Files)
```js
// index.js — public API of a feature module
export { UserService }       from './user.service.js';
export { UserComponent }     from './user.component.js';
export { User }              from './user.model.js';
export { USER_ROLES }        from './user.constants.js';
export { default as UserUtils } from './user.utils.js';

// Re-export everything
export * from './user.helpers.js';
```

```js
// Consumer imports from barrel
import { UserService, User, USER_ROLES } from './users/index.js';
// Instead of multiple paths:
// import { UserService } from './users/user.service.js';
// import { User } from './users/user.model.js';
```

### Angular Relevance
```ts
// Angular feature module barrel file (index.ts)
// src/app/features/users/index.ts
export * from './user.service';
export * from './user.component';
export * from './user.model';
export * from './user-list.component';
```

---

## 9.3 Dynamic Imports — Lazy Loading

```js
// Static import — loaded at startup (in the bundle)
import { HeavyChart } from './chart.js';

// Dynamic import — loaded on demand (code splitting)
async function loadChart() {
  const { HeavyChart } = await import('./chart.js');
  new HeavyChart('#container');
}

// Conditional loading
if (userIsAdmin) {
  const { AdminPanel } = await import('./admin.js');
  new AdminPanel();
}

// With error handling
try {
  const module = await import('./optional-feature.js');
  module.init();
} catch {
  console.log('Optional feature not available');
}
```

### Angular Lazy Loading
```ts
// app-routing.module.ts — lazy load entire feature modules
const routes: Routes = [
  { path: 'admin',  loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule) },
  { path: 'users',  loadChildren: () => import('./users/users.routes').then(m => m.USER_ROUTES) },
];

// Angular 17+ — standalone component lazy loading
{
  path: 'dashboard',
  loadComponent: () => import('./dashboard/dashboard.component').then(c => c.DashboardComponent)
}
```

---

## 9.4 Modern ES6+ Features — Complete Reference

### Destructuring (recap + advanced)
```js
// Array destructuring with iterator
const [first, ...rest] = new Set([1, 2, 3, 4]);
// first = 1, rest = [2, 3, 4]

// Swap
let a = 1, b = 2;
[a, b] = [b, a];

// Ignore return values
const [,, third] = [10, 20, 30];

// Nested
const { meta: { total, page } } = apiResponse;
```

### Template Literals — Advanced
```js
// Tagged templates — custom string processing
function highlight(strings, ...values) {
  return strings.reduce((result, str, i) => {
    const value = values[i - 1];
    return result + `<mark>${value}</mark>` + str;
  });
}

const name = 'Alice';
const role = 'admin';
highlight`User ${name} has role ${role}`;
// 'User <mark>Alice</mark> has role <mark>admin</mark>'

// Real-world: SQL template tag (prevents injection)
sql`SELECT * FROM users WHERE id = ${userId}`;
// Tag function sanitizes userId before interpolating
```

### Symbol
```js
// Unique identifier — never equal to anything else
const id = Symbol('id');
const id2 = Symbol('id');
id === id2; // false — always unique

// Use as object key — won't clash with string keys
const obj = {
  [Symbol('private')]: 'hidden',
  name: 'public'
};
Object.keys(obj);      // ['name'] — Symbols not in keys()
Object.getOwnPropertySymbols(obj); // [Symbol(private)]

// Well-known Symbols — customise built-in behaviour
class Range {
  constructor(from, to) { this.from = from; this.to = to; }

  [Symbol.iterator]() {
    let current = this.from;
    const to = this.to;
    return {
      next() {
        return current <= to
          ? { value: current++, done: false }
          : { value: undefined, done: true };
      }
    };
  }
}

[...new Range(1, 5)]; // [1, 2, 3, 4, 5]
for (const n of new Range(1, 3)) console.log(n); // 1, 2, 3
```

### Proxy & Reflect
```js
// Proxy — intercept fundamental object operations
const handler = {
  get(target, prop) {
    if (prop in target) return target[prop];
    throw new ReferenceError(`Property "${prop}" does not exist`);
  },
  set(target, prop, value) {
    if (typeof value !== 'number') throw new TypeError('Only numbers allowed');
    target[prop] = value;
    return true; // must return true to indicate success
  }
};

const nums = new Proxy({}, handler);
nums.x = 10;    // ✅
nums.y = 'hi';  // ❌ TypeError
nums.x;         // 10
nums.z;         // ❌ ReferenceError

// Reflect — mirrors Proxy traps; proper way to forward operations
const loggingProxy = new Proxy(target, {
  get(obj, prop) {
    console.log(`Getting: ${prop}`);
    return Reflect.get(obj, prop);  // proper forwarding
  },
  set(obj, prop, value) {
    console.log(`Setting: ${prop} = ${value}`);
    return Reflect.set(obj, prop, value);
  }
});
```

### Angular Relevance
> Angular's reactive forms, change detection, and `@Input()` setters are conceptually similar to Proxy traps — intercepting reads and writes. Vue 3's reactivity system uses proxies directly.

---

## 9.5 Optional Chaining & Nullish Coalescing (Deep Dive)

```js
// Optional chaining — stops at first null/undefined
const city = user?.address?.city;            // undefined (no error)
const len  = users?.length;                  // safe on nullable array
const val  = config?.getValue?.();          // safe method call
const item = arr?.[0];                       // safe index access

// Nullish coalescing — fallback only for null/undefined (not 0, '')
const name    = user.name ?? 'Anonymous';    // not '' triggered
const count   = response.count ?? 0;        // not 0 triggered
const timeout = config.timeout ?? 3000;

// Nullish assignment ??=
user.name ??= 'Guest';    // assigns only if null/undefined

// Logical assignment
user.role  ||= 'viewer';  // assigns if falsy
user.score &&= user.score * 1.1; // assigns if truthy
```

---

## 9.6 ES2020–ES2025 Features

### ES2020
```js
// BigInt
const big = 9007199254740991n;
big + 1n; // 9007199254740992n — no precision loss

// globalThis — consistent global across environments
// window (browser), global (Node), self (Worker) → all normalised
globalThis.fetch;

// Promise.allSettled — covered in Module 6

// String.matchAll
const str = 'test1 test2 test3';
const matches = [...str.matchAll(/test(\d)/g)];
matches[0][1]; // '1'
```

### ES2021
```js
// String.replaceAll
'a-b-c'.replaceAll('-', '_'); // 'a_b_c'

// Logical assignment: ??=  ||=  &&=

// Numeric separators — readability
const billion   = 1_000_000_000;
const hex       = 0xFF_FF_FF;
const bytes     = 0b1111_0000;
```

### ES2022
```js
// Class fields & private methods (covered in Module 7)
class Foo {
  #privateField = 42;
  publicField = 'public';
  static staticField = 'static';
  #privateMethod() { return this.#privateField; }
}

// Array.at() / String.at() — negative indexing
[1, 2, 3].at(-1);      // 3
'hello'.at(-1);        // 'o'
[1, 2, 3].at(0);       // 1

// Object.hasOwn — preferred over hasOwnProperty
Object.hasOwn(obj, 'name'); // true/false

// Top-level await (in modules)
const data = await fetch('/api').then(r => r.json());  // no async wrapper needed
```

### ES2023
```js
// Array find from end
[1, 2, 3, 2, 1].findLast(x => x === 2);       // 2 (last occurrence)
[1, 2, 3, 2, 1].findLastIndex(x => x === 2);  // 3

// Array toSorted / toReversed / toSpliced — non-mutating alternatives
const arr = [3, 1, 2];
arr.toSorted();          // [1, 2, 3] — new array
arr.toReversed();        // [2, 1, 3] — new array
arr;                     // [3, 1, 2] — original unchanged!
arr.with(1, 99);         // [3, 99, 2] — replace at index, new array

// These are the immutable array alternatives Angular loves
```

### ES2024
```js
// Object.groupBy — group array into object of arrays
const users = [
  { name: 'Alice', role: 'admin' },
  { name: 'Bob',   role: 'user'  },
  { name: 'Eve',   role: 'admin' },
];
const byRole = Object.groupBy(users, u => u.role);
// { admin: [Alice, Eve], user: [Bob] }

// Map.groupBy
const mapGrouped = Map.groupBy(users, u => u.role);

// Promise.withResolvers — cleaner external resolve/reject
const { promise, resolve, reject } = Promise.withResolvers();
setTimeout(() => resolve('done'), 1000);
await promise; // 'done'
```

---

## 9.7 Module Patterns in Angular

### Angular Module System (NgModule — Classic)
```ts
@NgModule({
  declarations: [UserComponent, UserListComponent],  // components/pipes/directives
  imports:      [CommonModule, RouterModule, HttpClientModule],
  providers:    [UserService],
  exports:      [UserComponent]  // make available to importing modules
})
export class UserModule {}
```

### Standalone Components (Angular 17+ — Modern)
```ts
// No NgModule needed
@Component({
  standalone: true,
  imports: [CommonModule, RouterLink, AsyncPipe],
  template: `...`
})
export class UserComponent {}

// Bootstrap standalone app
bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    provideAnimations()
  ]
});
```

### Tree-Shakeable Providers
```ts
// ✅ providedIn: 'root' — no need to add to module providers
// Service is tree-shaken if never injected
@Injectable({ providedIn: 'root' })
export class UserService {}

// Scoped to a specific module / component
@Injectable({ providedIn: UserModule })
export class UserHelperService {}
```

---

## 9.8 Barrel Files & Import Organisation

### Project Structure with Barrels
```
src/app/
├── core/
│   ├── services/
│   │   ├── auth.service.ts
│   │   ├── http.service.ts
│   │   └── index.ts       ← barrel
│   ├── guards/
│   │   └── index.ts
│   └── index.ts           ← feature barrel
├── shared/
│   ├── components/
│   │   └── index.ts
│   └── index.ts
└── features/
    └── users/
        ├── user.model.ts
        ├── user.service.ts
        ├── user.component.ts
        └── index.ts        ← barrel
```

```ts
// src/app/features/users/index.ts
export * from './user.model';
export * from './user.service';
export { UserComponent } from './user.component';

// Consumer — clean single import
import { User, UserService, UserComponent } from '@app/features/users';
```

### Path Aliases (tsconfig.json)
```json
{
  "compilerOptions": {
    "paths": {
      "@app/*":    ["src/app/*"],
      "@core/*":   ["src/app/core/*"],
      "@shared/*": ["src/app/shared/*"],
      "@env/*":    ["src/environments/*"]
    }
  }
}
```

```ts
// ✅ Clean absolute imports — no ../../.. hell
import { AuthService }    from '@core/services';
import { ButtonComponent } from '@shared/components';
import { environment }    from '@env/environment';
```

---

## 9.9 CommonJS vs ES Modules

| Feature            | CommonJS (`require`)            | ES Modules (`import`)           |
|--------------------|----------------------------------|----------------------------------|
| Syntax             | `require()` / `module.exports`  | `import` / `export`             |
| Loading            | Synchronous                     | Asynchronous                    |
| Tree-shaking       | ❌ Not supported                | ✅ Supported                    |
| Top-level `await`  | ❌                              | ✅ (in module files)            |
| Live bindings      | ❌ Copied at require time       | ✅ Live references               |
| Used in            | Node.js (legacy)                | Browser, modern Node, Angular   |

```js
// CommonJS
const fs = require('fs');
module.exports = { myFn };

// ES Modules
import fs from 'fs';
export { myFn };

// Interop in Node.js — .mjs extension or "type": "module" in package.json
```

---

## 9.10 Common Mistakes & Best Practices

```js
// 1. Circular imports — A imports B, B imports A
// symptoms: undefined values, hard-to-debug errors
// Fix: extract shared logic to a third module C

// 2. Over-using barrel files — slows build, harder to trace
// Balance: barrel per feature, not per every file

// 3. Importing from wrong level
import { UserService } from '../../../core/services/user.service'; // ❌ deep path
import { UserService } from '@core/services';                     // ✅ alias

// 4. Star imports in production
import * as _ from 'lodash'; // ❌ imports entire library
import { debounce } from 'lodash'; // ✅ tree-shakeable

// 5. Top-level await blocking
// In modules, top-level await blocks execution of importing modules
// Use carefully — prefer async init functions

// 6. Default export naming inconsistency
// Default exports can be renamed by importer:
import Whatever from './user'; // 'Whatever' could be anything
// Named exports enforce consistent naming — prefer named exports
```

---

## Module 9 — Summary Notes

| Concept                  | Key Takeaway                                                                  |
|--------------------------|------------------------------------------------------------------------------|
| Named vs default exports | Prefer named exports — consistent naming, better tree-shaking               |
| Barrel files (`index.ts`) | Clean public API per feature — avoid deep import paths                     |
| Dynamic `import()`       | Code-splitting at the module level — foundation of Angular lazy loading     |
| `Symbol`                 | Unique keys; customise built-in behaviour via well-known Symbols            |
| `Proxy` / `Reflect`      | Intercept object operations — used in reactivity systems                    |
| `Array.at()` / `.with()` | Modern non-mutating array access/update                                     |
| `Object.groupBy`         | ES2024 — replace custom `reduce` grouping patterns                         |
| `toSorted/toReversed`    | ES2023 — immutable array alternatives (Angular-friendly)                   |
| Path aliases             | Use `@core`, `@shared` etc. in `tsconfig` — eliminate relative path hell   |
| Standalone components    | Angular 17+ — no NgModule, direct imports, better tree-shaking             |

---

## Module 9 — Checkpoint Tasks

- [ ] Create a feature module with a barrel `index.ts` and configure a `tsconfig` path alias for it.
- [ ] Write a tagged template literal `css\`...\`` that strips leading whitespace and returns a string.
- [ ] Implement a `createReactiveObject(obj)` using `Proxy` that logs every get and set operation.
- [ ] Convert a routes config file to use `loadComponent` lazy loading for every route.
- [ ] Refactor a component using deep relative imports to use path aliases.
- [ ] Write a `groupBy(arr, keyFn)` utility using `Map.groupBy` with a fallback `reduce` for older environments.

---

> **Module 9 complete.** Type `continue` to proceed to **Module 10 – Functional Programming.**

---

# Module 10 – Functional Programming

---

## 10.1 What is Functional Programming?

### Simple Explanation
**Functional Programming (FP)** is a style of writing code using **pure functions**, **immutable data**, and **function composition** — avoiding shared state and side effects.

### Deep Explanation
FP treats computation as the evaluation of mathematical functions. Key principles:

| Principle              | Meaning                                                            |
|------------------------|--------------------------------------------------------------------|
| **Pure functions**     | Same input → same output; no side effects                         |
| **Immutability**       | Never mutate data — always return new copies                      |
| **First-class functions** | Functions are values — pass, return, store them               |
| **Higher-order functions** | Functions that take/return functions                         |
| **Function composition** | Combine small functions to build larger ones                   |
| **Declarative style**  | Describe *what* to do, not *how* to do it                        |

### Why It Matters for Angular
Angular's entire reactive model — RxJS, `pipe()`, operators, pure pipes, `OnPush` change detection — is rooted in functional programming. Mastering FP makes you a dramatically better Angular developer.

---

## 10.2 Pure Functions (Deep Dive)

```js
// Pure — deterministic, no side effects
const add = (a, b) => a + b;
const double = x => x * 2;
const square = x => x ** 2;

// Impure — modifies external state
let total = 0;
const addToTotal = n => { total += n; };  // side effect

// Impure — depends on external state
const getDiscount = () => config.discount * price; // external dependency

// Impure — I/O (inherently impure — isolate at boundaries)
const saveUser = async user => await db.save(user);

// Making impure logic pure — push side effects to the edges
const calculateTotal = (items) =>
  items.reduce((sum, item) => sum + item.price, 0); // pure calculation

// Call impure function at the boundary
async function checkout(cart) {
  const total = calculateTotal(cart.items); // pure
  await saveOrder({ ...cart, total });       // impure — pushed to edge
}
```

---

## 10.3 Immutability in Practice

```js
// Primitives are immutable by nature
let name = 'Alice';
name.toUpperCase(); // returns new string, name unchanged

// Objects — must be handled carefully
const original = { name: 'Alice', address: { city: 'NYC' } };

// ❌ Mutating
original.name = 'Bob';

// ✅ Immutable update
const updated = { ...original, name: 'Bob' };

// ✅ Nested immutable update
const movedUser = {
  ...original,
  address: { ...original.address, city: 'LA' }
};

// Arrays — immutable operations
const list = [1, 2, 3];

const added   = [...list, 4];                         // add
const removed = list.filter(x => x !== 2);            // remove
const mapped  = list.map(x => x * 2);                 // transform
const sorted  = list.toSorted((a, b) => b - a);       // ES2023 immutable sort

// Deep freeze for true immutability (development/testing use)
function deepFreeze(obj) {
  Object.getOwnPropertyNames(obj).forEach(name => {
    const value = obj[name];
    if (value && typeof value === 'object') deepFreeze(value);
  });
  return Object.freeze(obj);
}

const config = deepFreeze({ db: { host: 'localhost', port: 5432 } });
config.db.host = 'other'; // silently fails (throws in strict mode)
```

---

## 10.4 Function Composition

### Simple Composition
```js
// compose — right to left (mathematical style)
const compose = (...fns) => x => fns.reduceRight((acc, fn) => fn(acc), x);

// pipe — left to right (readable style)
const pipe = (...fns) => x => fns.reduce((acc, fn) => fn(acc), x);

// Individual pure functions
const trim        = str => str.trim();
const toLowerCase = str => str.toLowerCase();
const removeSpaces = str => str.replace(/\s+/g, '-');
const addPrefix   = str => `user-${str}`;

// Compose into a pipeline
const createSlug = pipe(trim, toLowerCase, removeSpaces, addPrefix);

createSlug('  Hello World  '); // 'user-hello-world'

// Without pipe — nested and unreadable
addPrefix(removeSpaces(toLowerCase(trim('  Hello World  '))));
```

### Point-Free Style
```js
// Point-free — define transforms without mentioning the data argument
const users = [
  { name: 'alice', active: true  },
  { name: 'bob',   active: false },
  { name: 'eve',   active: true  },
];

// With argument
const activeNames = users
  .filter(u => u.active)
  .map(u => u.name);

// Point-free style
const isActive   = u => u.active;
const getName    = u => u.name;
const capitalize = s => s[0].toUpperCase() + s.slice(1);

const activeNames2 = users
  .filter(isActive)
  .map(getName)
  .map(capitalize);
// ['Alice', 'Eve']
```

### Angular / RxJS Pipe
```ts
// RxJS pipe IS function composition
this.users$ = this.userService.getAll().pipe(
  filter(users => users.length > 0),       // like Array.filter
  map(users => users.filter(u => u.active)), // filter active
  map(users => users.sort((a,b) => a.name.localeCompare(b.name))),
  tap(users => console.log('Loaded:', users.length)),
  catchError(() => of([]))
);
```

---

## 10.5 Currying & Partial Application

### Currying
```js
// Transform f(a, b, c) into f(a)(b)(c)
const curry = fn => {
  return function curried(...args) {
    if (args.length >= fn.length) return fn(...args);
    return (...more) => curried(...args, ...more);
  };
};

const add = curry((a, b, c) => a + b + c);
add(1)(2)(3);   // 6
add(1, 2)(3);   // 6
add(1)(2, 3);   // 6
add(1, 2, 3);   // 6

// Real-world curried utilities
const multiply = curry((factor, num) => num * factor);
const double  = multiply(2);
const triple  = multiply(3);
const percent = multiply(0.01);

[10, 20, 30].map(double);   // [20, 40, 60]
[10, 20, 30].map(triple);   // [30, 60, 90]
```

### Partial Application
```js
// Fix some arguments upfront
const partial = (fn, ...presetArgs) =>
  (...laterArgs) => fn(...presetArgs, ...laterArgs);

const greet = (greeting, title, name) => `${greeting}, ${title} ${name}!`;

const greetHello = partial(greet, 'Hello');
const greetDr    = partial(greet, 'Hello', 'Dr.');

greetHello('Mr.', 'Smith');  // 'Hello, Mr. Smith!'
greetDr('Jones');            // 'Hello, Dr. Jones!'

// Angular use case
const logWithPrefix = partial(console.log, '[AppComponent]');
logWithPrefix('Initialized');     // '[AppComponent] Initialized'
logWithPrefix('User loaded', 42); // '[AppComponent] User loaded 42'
```

---

## 10.6 Functors & Monads (Conceptual)

### Functor — "mappable" container
A **functor** is any container you can `map` over without leaving the container.

```js
// Array is a functor
[1, 2, 3].map(x => x * 2); // [2, 4, 6] — still an array

// Promise is a functor (via .then)
Promise.resolve(5).then(x => x * 2); // Promise<10>

// Observable is a functor (RxJS map)
of(5).pipe(map(x => x * 2)); // Observable<10>

// Creating a simple Maybe functor (handles null safely)
class Maybe {
  constructor(value) { this._value = value; }
  static of(value) { return new Maybe(value); }

  isNothing() { return this._value == null; }

  map(fn) {
    return this.isNothing() ? this : Maybe.of(fn(this._value));
  }

  getOrElse(defaultValue) {
    return this.isNothing() ? defaultValue : this._value;
  }
}

const city = Maybe.of(user)
  .map(u => u.address)
  .map(a => a.city)
  .getOrElse('Unknown');
// No null reference errors — safe chained access
```

### Monad — flatMappable container
A monad is a functor that can also `flatMap` (chain operations that themselves return the container).

```js
// Promise is a monad — .then flattens nested Promises
Promise.resolve(1)
  .then(n => Promise.resolve(n + 1))  // returns Promise, not Promise<Promise>
  .then(n => console.log(n));          // 2

// Array flatMap
[1, 2, 3].flatMap(n => [n, -n]); // [1,-1, 2,-2, 3,-3]

// Observable is a monad — switchMap, mergeMap, concatMap
this.route.params.pipe(
  switchMap(({ id }) => this.userService.getUser(id)) // flatMap for Observables
);
```

---

## 10.7 Transducers (Advanced Composition)

```js
// Problem: chaining map/filter creates intermediate arrays
const result = [1,2,3,4,5,6,7,8,9,10]
  .filter(x => x % 2 === 0)   // → intermediate array [2,4,6,8,10]
  .map(x => x * x)             // → intermediate array [4,16,36,64,100]
  .filter(x => x > 20);        // → [36, 64, 100]

// Transducer — compose transforms without intermediate arrays
const filterEven   = reducer => (acc, x) => x % 2 === 0 ? reducer(acc, x) : acc;
const mapSquare    = reducer => (acc, x) => reducer(acc, x * x);
const filterGt20   = reducer => (acc, x) => x > 20 ? reducer(acc, x) : acc;

const append = (acc, x) => [...acc, x];
const xform  = pipe(filterEven, mapSquare, filterGt20);

[1,2,3,4,5,6,7,8,9,10].reduce(xform(append), []);
// [36, 64, 100] — single pass, no intermediate arrays
```

---

## 10.8 Recursion & Tail Call Optimization

```js
// Simple recursion
function factorial(n) {
  if (n <= 1) return 1;
  return n * factorial(n - 1);
}
factorial(5); // 120

// Stack overflow risk for large n
// factorial(100000) → Maximum call stack size exceeded

// Tail-recursive — last operation is the recursive call
function factorial(n, acc = 1) {
  if (n <= 1) return acc;
  return factorial(n - 1, n * acc); // tail call — JS engines can optimise
}

// Iterative alternative (safe for large inputs)
function factorial(n) {
  let result = 1;
  for (let i = 2; i <= n; i++) result *= i;
  return result;
}

// Recursive tree traversal — real-world use
function flattenTree(node) {
  if (!node.children?.length) return [node];
  return [node, ...node.children.flatMap(flattenTree)];
}

// Mutual recursion
const isEven = n => n === 0 ? true  : isOdd(n - 1);
const isOdd  = n => n === 0 ? false : isEven(n - 1);
```

---

## 10.9 Functional Error Handling

```js
// Result type — Either/Result monad pattern
class Ok {
  constructor(value)  { this.value = value; this.ok = true; }
  map(fn)     { return new Ok(fn(this.value)); }
  flatMap(fn) { return fn(this.value); }
  getOrElse() { return this.value; }
  match({ ok }) { return ok(this.value); }
}

class Err {
  constructor(error)  { this.error = error; this.ok = false; }
  map()       { return this; }  // propagate error, skip transforms
  flatMap()   { return this; }
  getOrElse(fallback) { return fallback; }
  match({ err }) { return err(this.error); }
}

// Usage
function divide(a, b) {
  return b === 0 ? new Err('Division by zero') : new Ok(a / b);
}

divide(10, 2)
  .map(x => x + 1)
  .match({
    ok:  val => console.log('Result:', val),   // 6
    err: msg => console.error('Error:', msg),
  });

divide(10, 0)
  .map(x => x + 1) // skipped
  .match({
    ok:  val => console.log('Result:', val),
    err: msg => console.error('Error:', msg),  // 'Division by zero'
  });
```

---

## 10.10 FP Patterns in Angular & RxJS

### Pure Pipes
```ts
// Pure pipe — Angular calls it ONLY when reference changes
@Pipe({ name: 'filterActive', pure: true })  // default: pure = true
export class FilterActivePipe implements PipeTransform {
  transform(users: User[], active: boolean): User[] {
    return users.filter(u => u.active === active);
  }
  // ✅ Pure function — same input = same output, no side effects
}
```

### OnPush Change Detection
```ts
// OnPush + immutable data = maximum performance
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  // Angular only checks when:
  // 1. @Input reference changes (not mutation!)
  // 2. An Observable used with async pipe emits
  // 3. Event fires inside the component
})
export class UserCardComponent {
  @Input() user!: User;

  // ❌ This won't trigger change detection with OnPush
  updateName() { this.user.name = 'Bob'; }

  // ✅ This will — new object reference
  updateName() { this.user = { ...this.user, name: 'Bob' }; }
}
```

### RxJS Functional Pipeline
```ts
// Build reusable operator pipelines
const activeUsersOperator = pipe(
  filter((users: User[]) => users.length > 0),
  map(users => users.filter(u => u.active)),
  map(users => users.sort((a, b) => a.name.localeCompare(b.name))),
  shareReplay(1)
);

// Apply to any Observable<User[]>
this.users$ = this.userService.getAll().pipe(activeUsersOperator);

// Pure selector functions (NgRx / Signals style)
const selectActiveAdmins = (users: User[]) =>
  users.filter(u => u.active && u.role === 'admin');

const selectUserNames = (users: User[]) =>
  users.map(u => `${u.firstName} ${u.lastName}`);

// Compose selectors
const selectActiveAdminNames = pipe(selectActiveAdmins, selectUserNames);
// (conceptually — with RxJS map)
```

---

## Module 10 — Summary Notes

| Concept                  | Key Takeaway                                                                        |
|--------------------------|-------------------------------------------------------------------------------------|
| Pure functions           | Same input → same output; no side effects — foundation of testability              |
| Immutability             | Always return new data — enables OnPush, undo/redo, time-travel debugging          |
| `pipe` / `compose`       | Combine small functions into readable data transformation pipelines                |
| Currying                 | Transform multi-arg functions into chains — enables partial application            |
| Point-free style         | Define transforms without naming the data — cleaner, more reusable                |
| Functor                  | Mappable container — Array, Promise, Observable all qualify                        |
| Monad                    | Functor with flatMap — chains operations that return containers (switchMap, .then) |
| Result type              | Functional error handling — no exceptions in the happy path                       |
| Pure pipes               | FP pure functions as Angular pipes — Angular only re-runs on reference change     |
| OnPush + immutability    | Maximum Angular performance — change detection only on new references             |

---

## Module 10 — Checkpoint Tasks

- [ ] Implement `pipe(...fns)` and `compose(...fns)` from scratch with full type safety.
- [ ] Build a reusable `Maybe` monad and use it to safely navigate a deeply nested config object.
- [ ] Implement a `Result<T, E>` type with `map`, `flatMap`, `match`, and `getOrElse`.
- [ ] Refactor a component with mutable state to use immutable updates and `OnPush` change detection.
- [ ] Create a reusable RxJS operator pipeline `activeUsersOperator` that can be applied to any `Observable<User[]>`.
- [ ] Build a `memoize` + `curry` combo — a curried function whose results are memoized per unique argument set.

---

> **Module 10 complete.** Type `continue` to proceed to **Module 11 – Performance Optimization.**

---

# Module 11 – Performance Optimization

---

## 11.1 The Performance Mindset

### Simple Explanation
Performance optimization means making code run **faster**, use **less memory**, and feel **snappier** to the user — without over-engineering.

### The Golden Rule
> **Measure first. Optimize second.** Never guess — profile and find the real bottleneck.

### Performance Pillars

| Pillar                  | Focus                                               |
|-------------------------|-----------------------------------------------------|
| **Rendering**           | How fast the browser paints the screen             |
| **JavaScript execution**| How fast your code runs                            |
| **Memory**              | Avoiding leaks and unnecessary allocations         |
| **Network**             | Load less, load smarter                            |
| **Perceived performance**| Feel fast, even if not technically instant        |

---

## 11.2 JavaScript Engine Optimizations

### V8 Internals — What Helps the JIT Compiler
```js
// ✅ Monomorphic functions — same argument types every call
function add(a, b) { return a + b; }
add(1, 2);       // number + number — V8 specialises
add(3, 4);       // same shape — fast optimised path
add('a', 'b');   // different type — V8 deoptimises! (polymorphic)

// ✅ Consistent object shapes — V8 creates "hidden classes"
// Good — same property order and types
function createUser(name, age) {
  return { name, age };  // always same shape
}

// ❌ Bad — dynamic properties break hidden class optimisation
const user = {};
if (isAdmin) user.role = 'admin';  // only sometimes has .role
user.name = name;                  // different shape for different users

// ✅ Arrays — keep typed (don't mix types)
const nums = [1, 2, 3];       // V8: SMI (small integer) array — fastest
nums.push(3.14);               // V8 upgrades to double array
nums.push('text');             // V8 degrades to generic array — slow
```

### Deoptimisation Triggers
```js
// Things that prevent V8 optimisation:
// 1. try/catch inside hot loops (move try/catch outside)
// 2. arguments object (use rest params instead)
// 3. with statement (never use)
// 4. eval() (never use)
// 5. deleting object properties (sets value to undefined instead)
delete obj.prop;         // ❌ breaks hidden class
obj.prop = undefined;    // ✅ preserves shape
```

---

## 11.3 Measuring Performance

### `console.time` / `performance.now`
```js
// Simple timing
console.time('operation');
heavyComputation();
console.timeEnd('operation'); // 'operation: 12.34ms'

// High-resolution
const start = performance.now();
heavyComputation();
const end = performance.now();
console.log(`Took ${(end - start).toFixed(2)}ms`);

// Mark and measure (DevTools Performance panel)
performance.mark('start-render');
renderList();
performance.mark('end-render');
performance.measure('render-time', 'start-render', 'end-render');
```

### Chrome DevTools
- **Performance panel** — record, then inspect flame chart, long tasks (>50ms), layout thrashing.
- **Memory panel** — heap snapshots, allocation timeline, detect leaks.
- **Coverage panel** — % of JS/CSS actually used on load.
- **Lighthouse** — automated audit: performance score, LCP, FID, CLS, TTI.

---

## 11.4 Memoization & Caching

```js
// Memoize — cache results by arguments
function memoize(fn) {
  const cache = new Map();
  return function(...args) {
    const key = JSON.stringify(args);
    if (cache.has(key)) return cache.get(key);
    const result = fn.apply(this, args);
    cache.set(key, result);
    return result;
  };
}

const expensiveFibonacci = memoize(function fib(n) {
  if (n <= 1) return n;
  return expensiveFibonacci(n - 1) + expensiveFibonacci(n - 2);
});

expensiveFibonacci(40); // computed once
expensiveFibonacci(40); // instant — from cache

// LRU Cache — bounded memoization (evict oldest when full)
class LRUCache {
  #cache;
  #maxSize;

  constructor(maxSize = 100) {
    this.#cache = new Map();
    this.#maxSize = maxSize;
  }

  get(key) {
    if (!this.#cache.has(key)) return undefined;
    // Move to end (most recently used)
    const value = this.#cache.get(key);
    this.#cache.delete(key);
    this.#cache.set(key, value);
    return value;
  }

  set(key, value) {
    if (this.#cache.has(key)) this.#cache.delete(key);
    else if (this.#cache.size >= this.#maxSize) {
      // Delete least recently used (first inserted)
      this.#cache.delete(this.#cache.keys().next().value);
    }
    this.#cache.set(key, value);
  }
}
```

### Angular Relevance
```ts
// Pure pipes ARE memoization — Angular caches pipe output by reference
@Pipe({ name: 'formatDate', pure: true })
export class FormatDatePipe implements PipeTransform {
  private cache = new Map<string, string>();

  transform(date: Date, format: string): string {
    const key = `${date.getTime()}-${format}`;
    if (this.cache.has(key)) return this.cache.get(key)!;
    const result = formatDate(date, format, 'en-US');
    this.cache.set(key, result);
    return result;
  }
}
```

---

## 11.5 Debounce & Throttle

### Debounce — wait until activity stops
```js
// Execute only after N ms of inactivity
function debounce(fn, delay) {
  let timerId;
  return function(...args) {
    clearTimeout(timerId);
    timerId = setTimeout(() => fn.apply(this, args), delay);
  };
}

// Use: search input, resize handler, form auto-save
const searchDebounced = debounce((query) => {
  fetchSearchResults(query);
}, 300);

input.addEventListener('input', e => searchDebounced(e.target.value));
// fetchSearchResults called only 300ms after user stops typing
```

### Throttle — limit execution rate
```js
// Execute at most once per N ms
function throttle(fn, limit) {
  let inThrottle = false;
  return function(...args) {
    if (inThrottle) return;
    fn.apply(this, args);
    inThrottle = true;
    setTimeout(() => { inThrottle = false; }, limit);
  };
}

// Use: scroll handler, mouse move, button spam prevention
const throttledScroll = throttle(() => {
  updateScrollProgress();
}, 100);

window.addEventListener('scroll', throttledScroll);
```

### Angular with RxJS (preferred approach)
```ts
// RxJS debounceTime — much cleaner than manual debounce
searchControl.valueChanges.pipe(
  debounceTime(300),
  distinctUntilChanged(),
  switchMap(query => this.searchService.search(query))
).subscribe(results => this.results = results);

// RxJS throttleTime
fromEvent(window, 'scroll').pipe(
  throttleTime(100),
  map(() => window.scrollY)
).subscribe(y => this.scrollPosition = y);
```

---

## 11.6 Virtual Scrolling & Large Lists

```js
// Naive rendering — creates 10,000 DOM nodes
items.forEach(item => {
  const li = document.createElement('li');
  li.textContent = item.name;
  list.appendChild(li); // ❌ 10,000 DOM nodes = slow paint + memory
});

// Virtual scrolling concept
// Only render items visible in the viewport + small buffer
// As user scrolls, recycle DOM nodes and update their content

// Manual virtual scroll (simplified)
class VirtualList {
  constructor({ container, itemHeight, items, renderItem }) {
    this.container = container;
    this.itemHeight = itemHeight;
    this.items = items;
    this.renderItem = renderItem;
    this.container.style.position = 'relative';

    // Total height spacer — makes scrollbar correct size
    this.spacer = document.createElement('div');
    this.spacer.style.height = `${items.length * itemHeight}px`;
    container.appendChild(this.spacer);

    this.visibleContainer = document.createElement('div');
    this.visibleContainer.style.position = 'absolute';
    this.visibleContainer.style.top = '0';
    this.visibleContainer.style.width = '100%';
    container.appendChild(this.visibleContainer);

    container.addEventListener('scroll', () => this.render());
    this.render();
  }

  render() {
    const scrollTop = this.container.scrollTop;
    const viewportHeight = this.container.clientHeight;
    const startIndex = Math.floor(scrollTop / this.itemHeight);
    const endIndex   = Math.min(
      startIndex + Math.ceil(viewportHeight / this.itemHeight) + 1,
      this.items.length
    );

    this.visibleContainer.style.transform =
      `translateY(${startIndex * this.itemHeight}px)`;
    this.visibleContainer.innerHTML = '';

    for (let i = startIndex; i < endIndex; i++) {
      this.visibleContainer.appendChild(this.renderItem(this.items[i], i));
    }
  }
}
```

### Angular CDK Virtual Scrolling
```ts
// ✅ Use Angular CDK — do not reinvent
import { ScrollingModule } from '@angular/cdk/scrolling';

@Component({
  template: `
    <cdk-virtual-scroll-viewport itemSize="50" style="height: 500px">
      <div *cdkVirtualFor="let item of items">
        {{ item.name }}
      </div>
    </cdk-virtual-scroll-viewport>
  `
})
export class UserListComponent {
  items = largeArray; // 100,000 items — rendered only ~10 at a time
}
```

---

## 11.7 Layout Thrashing

### What Is It?
**Layout thrashing** (forced synchronous layout) occurs when you read a layout property (which forces the browser to recalculate layout), then immediately write to the DOM (invalidating layout), and repeat in a loop.

```js
// ❌ Layout thrashing — read/write/read/write in a loop
const boxes = document.querySelectorAll('.box');
boxes.forEach(box => {
  const width = box.offsetWidth;     // READ — forces layout recalc
  box.style.width = width + 10 + 'px'; // WRITE — invalidates layout
  // Next iteration: READ again — forces recalc of dirty layout
});

// ✅ Batch reads, then batch writes (FastDOM pattern)
const widths = [...boxes].map(box => box.offsetWidth); // all reads first
boxes.forEach((box, i) => {
  box.style.width = widths[i] + 10 + 'px';             // all writes after
});

// ✅ Even better — requestAnimationFrame batching
function updateBoxes() {
  const widths = [...boxes].map(b => b.offsetWidth); // read
  requestAnimationFrame(() => {
    boxes.forEach((b, i) => { b.style.width = widths[i] + 10 + 'px'; }); // write
  });
}
```

### Properties That Trigger Layout
Reading these forces a synchronous layout recalculation:
`offsetWidth/Height`, `clientWidth/Height`, `scrollTop/Left`, `getBoundingClientRect()`, `getComputedStyle()`

---

## 11.8 Web Workers

```js
// Heavy computation blocks the main thread
// Move it to a Web Worker — runs in a separate thread

// worker.js
self.addEventListener('message', ({ data }) => {
  const result = heavyComputation(data); // runs off main thread
  self.postMessage(result);
});

// main.js
const worker = new Worker('./worker.js');

worker.postMessage(largeDataset);  // send data to worker

worker.addEventListener('message', ({ data }) => {
  console.log('Worker result:', data); // receive result
  worker.terminate(); // clean up
});

// Transferable objects — zero-copy transfer (for large ArrayBuffers)
const buffer = new ArrayBuffer(1024 * 1024); // 1MB
worker.postMessage(buffer, [buffer]);   // transfer ownership — buffer now empty in main thread
```

### Angular with Web Workers
```ts
// Angular CLI generates worker scaffolding:
// ng generate web-worker app

// app.worker.ts
addEventListener('message', ({ data }) => {
  const result = processLargeDataset(data);
  postMessage(result);
});

// component.ts
if (typeof Worker !== 'undefined') {
  const worker = new Worker(new URL('./app.worker', import.meta.url));
  worker.postMessage(this.dataset);
  worker.onmessage = ({ data }) => {
    this.processedData = data;
    this.cdr.markForCheck(); // trigger OnPush change detection
  };
}
```

---

## 11.9 Bundle & Load Optimization

### Code Splitting Strategies
```ts
// 1. Route-based (most important — Angular does this automatically)
{ path: 'reports', loadChildren: () => import('./reports/reports.routes') }

// 2. Component-level (for heavy infrequently-used components)
async loadEditor() {
  const { RichEditorComponent } = await import('./rich-editor.component');
  this.editorComponent = RichEditorComponent;
}

// 3. Feature flags — load polyfills / premium features on demand
if (this.featureFlags.premiumCharts) {
  const { ChartModule } = await import('./chart.module');
}
```

### Tree-Shaking Best Practices
```ts
// ✅ Named imports — tree-shakeable
import { map, filter, debounceTime } from 'rxjs/operators';

// ❌ Namespace imports — prevents tree-shaking
import * as _ from 'lodash';

// ✅ Specific lodash imports
import debounce from 'lodash/debounce';
import throttle from 'lodash/throttle';

// ✅ providedIn: 'root' — tree-shaken if not used
@Injectable({ providedIn: 'root' })
export class AnalyticsService {} // not included in bundle if never injected
```

### Preloading Strategies
```ts
// Preload all lazy modules after app is interactive
RouterModule.forRoot(routes, {
  preloadingStrategy: PreloadAllModules
})

// Custom smart preloading — only modules the user is likely to visit
@Injectable({ providedIn: 'root' })
class SelectivePreload implements PreloadingStrategy {
  preload(route: Route, load: () => Observable<any>) {
    return route.data?.['preload'] ? load() : EMPTY;
  }
}

const routes = [
  { path: 'admin', loadChildren: ..., data: { preload: isAdmin } },
  { path: 'reports', loadChildren: ..., data: { preload: false } },
];
```

---

## 11.10 Angular-Specific Performance

### Change Detection Optimization
```ts
// 1. OnPush — most impactful single optimization
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush
})

// 2. trackBy — avoid re-rendering unchanged list items
@Component({
  template: `
    <div *ngFor="let user of users; trackBy: trackById">
      {{ user.name }}
    </div>
  `
})
export class UserListComponent {
  trackById = (index: number, user: User) => user.id;
  // Without trackBy: Angular destroys + recreates ALL DOM nodes on data change
  // With trackBy: Angular only updates changed items
}

// 3. Run outside NgZone for non-UI work
constructor(private zone: NgZone) {}

startHeavyProcessing() {
  this.zone.runOutsideAngular(() => {
    // No change detection triggered during this work
    const result = processData(this.data);
    this.zone.run(() => {
      this.result = result; // re-enter zone only to update state
    });
  });
}

// 4. Detach change detector for components updated manually
constructor(private cdr: ChangeDetectorRef) {}

ngOnInit() {
  this.cdr.detach(); // completely detach from change detection tree
  setInterval(() => {
    this.data = getLatestData();
    this.cdr.detectChanges(); // manually trigger when needed
  }, 5000);
}
```

### Angular Signals (Angular 16+)
```ts
// Signals = fine-grained reactivity — only re-render what changed
import { signal, computed, effect } from '@angular/core';

@Component({
  template: `
    <p>Count: {{ count() }}</p>
    <p>Double: {{ double() }}</p>
    <button (click)="increment()">+</button>
  `
})
export class CounterComponent {
  count  = signal(0);
  double = computed(() => this.count() * 2);  // auto-updates

  increment() {
    this.count.update(n => n + 1); // fine-grained update — no full check
  }
}

// effect — run side effects when signals change
effect(() => {
  console.log('Count changed:', this.count()); // auto-tracks dependencies
});
```

### Pipe vs Method in Template
```html
<!-- ❌ Method in template — called on EVERY change detection cycle -->
<div>{{ formatUser(user) }}</div>    <!-- formatUser() called constantly -->

<!-- ✅ Pure pipe — called only when input reference changes -->
<div>{{ user | formatUser }}</div>   <!-- only when user object changes -->

<!-- ✅ Precomputed in component -->
<div>{{ formattedUser }}</div>       <!-- computed once in ngOnChanges -->
```

---

## 11.11 Memory Leak Prevention

```ts
// Common Angular memory leaks and fixes:

// 1. Unsubscribed Observables
// ❌
this.service.data$.subscribe(d => this.data = d); // never unsubscribed

// ✅ Option A — takeUntilDestroyed (Angular 16+)
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
this.service.data$.pipe(takeUntilDestroyed()).subscribe(d => this.data = d);

// ✅ Option B — async pipe (auto-unsubscribes)
data$ = this.service.data$; // in template: {{ data$ | async }}

// ✅ Option C — manual with destroy$
private destroy$ = new Subject<void>();
this.service.data$.pipe(takeUntil(this.destroy$)).subscribe(d => this.data = d);
ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

// 2. Event listeners not removed
ngOnInit() {
  this.clickHandler = () => this.handleClick();
  document.addEventListener('click', this.clickHandler);
}
ngOnDestroy() {
  document.removeEventListener('click', this.clickHandler);
}

// 3. Intervals not cleared
ngOnInit()  { this.timer = setInterval(() => this.poll(), 5000); }
ngOnDestroy() { clearInterval(this.timer); }

// 4. Large data held in service
@Injectable({ providedIn: 'root' })
export class CacheService implements OnDestroy {
  private cache = new Map();
  clear() { this.cache.clear(); }
  ngOnDestroy() { this.clear(); }
}
```

---

## Module 11 — Summary Notes

| Technique                 | Impact    | When to Apply                                        |
|---------------------------|-----------|------------------------------------------------------|
| `OnPush` change detection | 🔥 High   | All components — default to it                      |
| `trackBy` in `*ngFor`     | 🔥 High   | Any list that updates frequently                    |
| Virtual scrolling         | 🔥 High   | Lists > 100 items                                    |
| Pure pipes over methods   | ⚡ Medium  | Any transformation shown in template                |
| `debounce`/`throttle`     | ⚡ Medium  | Input events, scroll, resize                        |
| Lazy loading routes       | ⚡ Medium  | All feature modules                                  |
| Memoization               | ⚡ Medium  | Expensive pure computations called repeatedly       |
| Web Workers               | ⚡ Medium  | CPU-heavy tasks (parsing, image processing)         |
| Angular Signals           | 🔥 High   | Fine-grained reactivity — future of Angular         |
| `runOutsideAngular`       | ⚡ Medium  | Timers, third-party libs, non-UI work               |
| Batch DOM reads/writes    | ⚡ Medium  | Any loop that reads + writes DOM properties         |
| `takeUntilDestroyed`      | 🛡 Safety  | All component-scoped subscriptions                  |

---

## Module 11 — Checkpoint Tasks

- [ ] Profile an Angular app with Chrome DevTools — identify and fix one long task (>50ms).
- [ ] Implement `debounce` and `throttle` from scratch and compare with RxJS equivalents.
- [ ] Refactor a component's `*ngFor` to add `trackBy` and measure the difference with 1000 items.
- [ ] Convert a default change detection component to `OnPush` — fix all the resulting issues.
- [ ] Move a heavy CSV parsing operation to a Web Worker — keep the UI responsive.
- [ ] Audit a component for memory leaks — fix all subscriptions, event listeners, and intervals.
- [ ] Implement an LRU cache with a max size of 50 entries.

---

> **Module 11 complete.** Type `continue` to proceed to **Module 12 – Advanced Engineering Patterns.**

---

# Module 12 – Advanced Engineering Patterns

---

## 12.1 Design Patterns Overview

### Simple Explanation
Design patterns are **proven, reusable solutions** to common software design problems. They are not code — they are templates for how to structure code.

### Categories

| Category      | Purpose                                      | Examples                             |
|---------------|----------------------------------------------|--------------------------------------|
| **Creational** | How objects are created                     | Singleton, Factory, Builder, Prototype |
| **Structural** | How objects are composed                    | Adapter, Decorator, Facade, Proxy    |
| **Behavioral** | How objects communicate                     | Observer, Strategy, Command, Chain of Responsibility |

---

## 12.2 Creational Patterns

### Singleton
```js
// Ensures only ONE instance exists
class ConfigService {
  static #instance = null;
  #config = {};

  constructor() {
    if (ConfigService.#instance) return ConfigService.#instance;
    ConfigService.#instance = this;
  }

  set(key, value) { this.#config[key] = value; }
  get(key)        { return this.#config[key]; }

  static getInstance() {
    if (!ConfigService.#instance) new ConfigService();
    return ConfigService.#instance;
  }
}

const a = ConfigService.getInstance();
const b = ConfigService.getInstance();
a === b; // true — same instance

// Angular equivalent: providedIn: 'root' services ARE singletons
@Injectable({ providedIn: 'root' })
export class AppConfigService {} // one instance for the entire app
```

### Factory
```js
// Create objects without specifying exact class
class UserFactory {
  static create(type, data) {
    switch (type) {
      case 'admin':   return new AdminUser(data);
      case 'guest':   return new GuestUser(data);
      case 'premium': return new PremiumUser(data);
      default:        throw new Error(`Unknown user type: ${type}`);
    }
  }
}

const user = UserFactory.create('admin', { name: 'Alice' });

// Abstract factory — family of related objects
class ThemeFactory {
  static create(theme) {
    const factories = { dark: DarkThemeFactory, light: LightThemeFactory };
    const factory = factories[theme];
    if (!factory) throw new Error(`Theme not found: ${theme}`);
    return new factory();
  }
}

// Angular — factory providers
{
  provide: LogService,
  useFactory: (env: Environment) =>
    env.production ? new ProdLogService() : new DevLogService(),
  deps: [Environment]
}
```

### Builder
```js
// Construct complex objects step by step
class QueryBuilder {
  #table = '';
  #conditions = [];
  #orderBy = null;
  #limit = null;
  #offset = null;

  from(table)          { this.#table = table;              return this; }
  where(condition)     { this.#conditions.push(condition); return this; }
  orderBy(col, dir='ASC') { this.#orderBy = `${col} ${dir}`; return this; }
  limit(n)             { this.#limit = n;                  return this; }
  offset(n)            { this.#offset = n;                 return this; }

  build() {
    if (!this.#table) throw new Error('Table is required');
    let query = `SELECT * FROM ${this.#table}`;
    if (this.#conditions.length) query += ` WHERE ${this.#conditions.join(' AND ')}`;
    if (this.#orderBy)           query += ` ORDER BY ${this.#orderBy}`;
    if (this.#limit)             query += ` LIMIT ${this.#limit}`;
    if (this.#offset)            query += ` OFFSET ${this.#offset}`;
    return query;
  }
}

const query = new QueryBuilder()
  .from('users')
  .where('active = true')
  .where('role = "admin"')
  .orderBy('name')
  .limit(20)
  .offset(40)
  .build();
// 'SELECT * FROM users WHERE active = true AND role = "admin" ORDER BY name ASC LIMIT 20 OFFSET 40'
```

---

## 12.3 Structural Patterns

### Decorator
```js
// Add behaviour without modifying the original class
function readonly(target, name, descriptor) {
  descriptor.writable = false;
  return descriptor;
}

function log(target, name, descriptor) {
  const original = descriptor.value;
  descriptor.value = function(...args) {
    console.log(`Calling ${name} with`, args);
    const result = original.apply(this, args);
    console.log(`${name} returned`, result);
    return result;
  };
  return descriptor;
}

class MathService {
  @log
  add(a, b) { return a + b; }
}

// Angular uses decorators extensively:
@Component({ ... })       // component metadata
@Injectable({ ... })      // DI registration
@Input()                  // property binding
@Output()                 // event emitter
@ViewChild(...)           // DOM query
@HostListener(...)        // DOM event binding
```

### Proxy Pattern (behavioral/structural)
```js
// Control access to an object (covered in Module 9 — Proxy/Reflect)
// Real-world: validation proxy
function createValidatedConfig(target, schema) {
  return new Proxy(target, {
    set(obj, prop, value) {
      const validator = schema[prop];
      if (validator && !validator(value)) {
        throw new TypeError(`Invalid value for ${prop}: ${value}`);
      }
      obj[prop] = value;
      return true;
    }
  });
}

const config = createValidatedConfig({}, {
  port:    v => Number.isInteger(v) && v > 0 && v < 65536,
  host:    v => typeof v === 'string' && v.length > 0,
  timeout: v => Number.isInteger(v) && v > 0,
});

config.port = 3000;   // ✅
config.port = 99999;  // ❌ TypeError: Invalid value for port: 99999
```

### Adapter
```js
// Bridge incompatible interfaces
// Old API
class OldPaymentGateway {
  makePayment(amount, currency, cardNumber) { ... }
}

// New interface your app expects
class NewPaymentAdapter {
  constructor(oldGateway) { this.gateway = oldGateway; }

  // New interface
  processTransaction({ amount, currency, card }) {
    return this.gateway.makePayment(amount, currency, card.number);
  }
}

// Angular equivalent — adapting HTTP response to app model
class UserAdapter {
  toModel(apiResponse): User {
    return {
      id:        apiResponse.user_id,
      name:      `${apiResponse.first_name} ${apiResponse.last_name}`,
      email:     apiResponse.email_address,
      createdAt: new Date(apiResponse.created_at * 1000)
    };
  }
}
```

### Facade
```js
// Simplified interface to a complex subsystem
class OrderFacade {
  constructor(
    private inventory: InventoryService,
    private payment: PaymentService,
    private shipping: ShippingService,
    private notification: NotificationService
  ) {}

  // Single method hides the complexity
  async placeOrder(cart, user, paymentInfo) {
    await this.inventory.reserve(cart.items);
    const charge = await this.payment.charge(cart.total, paymentInfo);
    const shipment = await this.shipping.schedule(cart.items, user.address);
    await this.notification.sendConfirmation(user.email, { charge, shipment });
    return { success: true, orderId: charge.orderId };
  }
}

// Consumer just calls one method
const result = await orderFacade.placeOrder(cart, user, payment);
```

---

## 12.4 Behavioral Patterns

### Observer / Pub-Sub
```js
// Objects subscribe to events — notified when state changes
class EventEmitter {
  #listeners = new Map();

  on(event, listener) {
    if (!this.#listeners.has(event)) this.#listeners.set(event, new Set());
    this.#listeners.get(event).add(listener);
    return () => this.off(event, listener); // return unsubscribe fn
  }

  off(event, listener) {
    this.#listeners.get(event)?.delete(listener);
  }

  emit(event, ...args) {
    this.#listeners.get(event)?.forEach(fn => fn(...args));
  }

  once(event, listener) {
    const wrapper = (...args) => {
      listener(...args);
      this.off(event, wrapper);
    };
    return this.on(event, wrapper);
  }
}

const bus = new EventEmitter();
const unsubscribe = bus.on('user:login', user => console.log('Logged in:', user.name));
bus.emit('user:login', { name: 'Alice' });  // 'Logged in: Alice'
unsubscribe(); // remove listener

// Angular: RxJS Subject is the EventEmitter equivalent
const events$ = new Subject<UserEvent>();
events$.subscribe(e => handleEvent(e));
events$.next({ type: 'login', user });
```

### Strategy
```js
// Swap algorithms at runtime
class Sorter {
  #strategy;

  setStrategy(strategy) { this.#strategy = strategy; }

  sort(data) {
    if (!this.#strategy) throw new Error('No strategy set');
    return this.#strategy.sort(data);
  }
}

class BubbleSortStrategy {
  sort(arr) { /* ... */ return arr; }
}
class QuickSortStrategy {
  sort(arr) { /* ... */ return arr; }
}
class MergeSortStrategy {
  sort(arr) { /* ... */ return arr; }
}

const sorter = new Sorter();
sorter.setStrategy(new QuickSortStrategy());
sorter.sort([3, 1, 2]);

// Angular — strategy via DI token
const VALIDATION_STRATEGY = new InjectionToken<ValidationStrategy>('VALIDATION');

@Injectable()
class FormService {
  constructor(@Inject(VALIDATION_STRATEGY) private strategy: ValidationStrategy) {}
  validate(data) { return this.strategy.validate(data); }
}

// Swap strategy by changing provider
{ provide: VALIDATION_STRATEGY, useClass: StrictValidationStrategy }
```

### Command
```js
// Encapsulate actions as objects — enables undo/redo, queuing
class CommandHistory {
  #history = [];
  #redoStack = [];

  execute(command) {
    command.execute();
    this.#history.push(command);
    this.#redoStack = []; // clear redo stack on new command
  }

  undo() {
    const command = this.#history.pop();
    if (command) {
      command.undo();
      this.#redoStack.push(command);
    }
  }

  redo() {
    const command = this.#redoStack.pop();
    if (command) {
      command.execute();
      this.#history.push(command);
    }
  }
}

class AddTextCommand {
  constructor(editor, text) { this.editor = editor; this.text = text; }
  execute() { this.editor.append(this.text); }
  undo()    { this.editor.removeLast(this.text.length); }
}

const history = new CommandHistory();
history.execute(new AddTextCommand(editor, 'Hello'));
history.execute(new AddTextCommand(editor, ' World'));
history.undo();   // removes ' World'
history.redo();   // re-adds ' World'
```

### Chain of Responsibility
```js
// Pass request through a chain of handlers
class Handler {
  #next = null;

  setNext(handler) { this.#next = handler; return handler; }

  handle(request) {
    if (this.#next) return this.#next.handle(request);
    return null;
  }
}

class AuthHandler extends Handler {
  handle(request) {
    if (!request.token) return { error: 'Unauthorized', status: 401 };
    return super.handle(request); // pass to next handler
  }
}

class RateLimitHandler extends Handler {
  handle(request) {
    if (request.rateLimitExceeded) return { error: 'Too Many Requests', status: 429 };
    return super.handle(request);
  }
}

class ValidationHandler extends Handler {
  handle(request) {
    if (!request.body?.name) return { error: 'Name required', status: 400 };
    return super.handle(request);
  }
}

class BusinessHandler extends Handler {
  handle(request) {
    return { data: processRequest(request), status: 200 };
  }
}

// Build chain
const auth        = new AuthHandler();
const rateLimit   = new RateLimitHandler();
const validation  = new ValidationHandler();
const business    = new BusinessHandler();

auth.setNext(rateLimit).setNext(validation).setNext(business);

auth.handle({ token: 'valid', body: { name: 'Alice' } }); // { data: ..., status: 200 }
auth.handle({ token: null });                              // { error: 'Unauthorized', status: 401 }
// Angular middleware/interceptors are a chain of responsibility
```

---

## 12.5 State Management Patterns

### Flux/Redux Pattern
```ts
// Unidirectional data flow: Action → Reducer → State → View → Action
type Action =
  | { type: 'ADD_USER';    payload: User   }
  | { type: 'REMOVE_USER'; payload: number }
  | { type: 'SET_LOADING'; payload: boolean };

interface State {
  users:   User[];
  loading: boolean;
  error:   string | null;
}

function reducer(state: State, action: Action): State {
  switch (action.type) {
    case 'ADD_USER':
      return { ...state, users: [...state.users, action.payload] };
    case 'REMOVE_USER':
      return { ...state, users: state.users.filter(u => u.id !== action.payload) };
    case 'SET_LOADING':
      return { ...state, loading: action.payload };
    default:
      return state;
  }
}

// Mini store
class Store<S> {
  #state: S;
  #listeners = new Set<() => void>();

  constructor(reducer: (s: S, a: any) => S, initialState: S) {
    this.#state = initialState;
    this.reducer = reducer;
  }

  getState()       { return this.#state; }
  subscribe(fn)    { this.#listeners.add(fn); return () => this.#listeners.delete(fn); }
  dispatch(action) {
    this.#state = this.reducer(this.#state, action);
    this.#listeners.forEach(fn => fn());
  }
}
```

### Repository Pattern
```ts
// Abstract data access — swap storage implementations easily
interface UserRepository {
  getAll():          Observable<User[]>;
  getById(id: number): Observable<User | null>;
  create(user: CreateUserDto): Observable<User>;
  update(id: number, user: Partial<User>): Observable<User>;
  delete(id: number): Observable<void>;
}

@Injectable()
class HttpUserRepository implements UserRepository {
  constructor(private http: HttpClient) {}

  getAll()                            { return this.http.get<User[]>('/api/users'); }
  getById(id: number)                  { return this.http.get<User>(`/api/users/${id}`); }
  create(dto: CreateUserDto)           { return this.http.post<User>('/api/users', dto); }
  update(id: number, data: Partial<User>) { return this.http.patch<User>(`/api/users/${id}`, data); }
  delete(id: number)                   { return this.http.delete<void>(`/api/users/${id}`); }
}

@Injectable()
class InMemoryUserRepository implements UserRepository {
  private users: User[] = [];
  getAll()    { return of(this.users); }
  getById(id) { return of(this.users.find(u => u.id === id) ?? null); }
  // ... (for testing)
}

// DI — swap without changing consumers
{ provide: UserRepository, useClass: environment.test
    ? InMemoryUserRepository
    : HttpUserRepository
}
```

---

## 12.6 Reactive Patterns with RxJS

### Subject Variants
```ts
// Subject — no initial value, only future values
const subject$ = new Subject<number>();
subject$.subscribe(console.log);
subject$.next(1); // 1
subject$.next(2); // 2

// BehaviorSubject — has current value, new subscribers get it immediately
const bs$ = new BehaviorSubject<number>(0); // initial = 0
bs$.subscribe(console.log);  // immediately: 0
bs$.next(1);                 // 1
bs$.getValue();              // 1 — synchronous access

// ReplaySubject — replays last N values to new subscribers
const rs$ = new ReplaySubject<number>(3); // buffer: 3
rs$.next(1); rs$.next(2); rs$.next(3); rs$.next(4);
rs$.subscribe(console.log); // 2, 3, 4 — last 3

// AsyncSubject — only emits LAST value, when complete
const as$ = new AsyncSubject<number>();
as$.subscribe(console.log);
as$.next(1); as$.next(2); as$.next(3);
as$.complete(); // now emits: 3 (only the last)
```

### State Service Pattern
```ts
@Injectable({ providedIn: 'root' })
export class UserStateService {
  private readonly _users = new BehaviorSubject<User[]>([]);
  private readonly _loading = new BehaviorSubject<boolean>(false);
  private readonly _error = new BehaviorSubject<string | null>(null);

  // Public read-only streams
  readonly users$   = this._users.asObservable();
  readonly loading$ = this._loading.asObservable();
  readonly error$   = this._error.asObservable();

  // Derived state (computed)
  readonly activeUsers$ = this.users$.pipe(
    map(users => users.filter(u => u.active)),
    shareReplay(1)
  );

  readonly vm$ = combineLatest({
    users:   this.users$,
    loading: this.loading$,
    error:   this.error$,
  });

  loadUsers() {
    this._loading.next(true);
    this._error.next(null);
    this.http.get<User[]>('/api/users').subscribe({
      next:     users => { this._users.next(users); this._loading.next(false); },
      error:    err   => { this._error.next(err.message); this._loading.next(false); }
    });
  }

  addUser(user: User) {
    this._users.next([...this._users.getValue(), user]);
  }
}
```

---

## 12.7 Interceptor & Middleware Pattern

```ts
// Angular HTTP Interceptor — cross-cutting concerns
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.auth.getToken();

    const authReq = token
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

    return next.handle(authReq);
  }
}

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      retry(1),
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401) this.router.navigate(['/login']);
        if (err.status === 403) this.router.navigate(['/forbidden']);
        return throwError(() => err);
      })
    );
  }
}

// Chain of interceptors — each calls next.handle() to pass control
// Auth → Error → Logging → Caching → Business Logic
```

---

## 12.8 Dependency Injection Deep Dive

```ts
// InjectionToken — for non-class values
const API_URL   = new InjectionToken<string>('API_URL');
const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');

// Provide via token
providers: [
  { provide: API_URL,    useValue: 'https://api.example.com' },
  { provide: APP_CONFIG, useFactory: () => environment.config },
]

// Inject via token
@Injectable()
class UserService {
  constructor(@Inject(API_URL) private apiUrl: string) {}
}

// inject() function (Angular 14+) — works outside constructor
@Component()
class UserComponent {
  private userService = inject(UserService);         // no constructor needed
  private apiUrl      = inject(API_URL);
  private router      = inject(Router);
  private destroy$    = inject(DestroyRef);
}

// Multi providers — inject array of all registered values
const HTTP_INTERCEPTORS = new InjectionToken<HttpInterceptor[]>('HTTP_INTERCEPTORS');
providers: [
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor,    multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor,   multi: true },
  { provide: HTTP_INTERCEPTORS, useClass: LoggingInterceptor, multi: true },
]
```

---

## 12.9 Error Handling Patterns

```ts
// Global error handler
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  constructor(private logger: LogService, private router: Router) {}

  handleError(error: Error) {
    this.logger.error(error);

    if (error instanceof HttpErrorResponse) {
      // Handle HTTP errors
    } else if (error instanceof TypeError) {
      // Handle type errors
    } else {
      this.router.navigate(['/error']);
    }
  }
}

providers: [{ provide: ErrorHandler, useClass: GlobalErrorHandler }]

// Component-level error boundary
@Component({
  template: `
    <ng-container *ngIf="error$ | async as error; else content">
      <app-error [message]="error" (retry)="retry()"></app-error>
    </ng-container>
    <ng-template #content>
      <ng-content></ng-content>
    </ng-template>
  `
})
export class ErrorBoundaryComponent {
  private errorSubject = new Subject<string | null>();
  error$ = this.errorSubject.asObservable();

  handleError(message: string) { this.errorSubject.next(message); }
  retry()                      { this.errorSubject.next(null); }
}
```

---

## 12.10 Micro-Frontend Pattern

```ts
// Module Federation — load remote Angular apps at runtime
// webpack.config.js (host app)
new ModuleFederationPlugin({
  remotes: {
    'mf-users':    'mfUsers@http://localhost:4201/remoteEntry.js',
    'mf-products': 'mfProducts@http://localhost:4202/remoteEntry.js',
  }
});

// Angular routing — lazy load remote micro-frontend
const routes: Routes = [
  {
    path: 'users',
    loadChildren: () => loadRemoteModule({
      remoteEntry: 'http://localhost:4201/remoteEntry.js',
      remoteName:  'mfUsers',
      exposedModule: './Module'
    }).then(m => m.UsersModule)
  }
];
```

---

## Module 12 — Summary Notes

| Pattern               | Category    | Angular Equivalent                                   |
|-----------------------|-------------|------------------------------------------------------|
| Singleton             | Creational  | `providedIn: 'root'` services                       |
| Factory               | Creational  | `useFactory` provider                               |
| Builder               | Creational  | `QueryBuilder`, `FormBuilder`                       |
| Decorator             | Structural  | `@Component`, `@Injectable`, `@Input`               |
| Adapter               | Structural  | HTTP response mapping services                      |
| Facade                | Structural  | Orchestrating service methods                       |
| Observer / Pub-Sub    | Behavioral  | `Subject`, `EventEmitter`, RxJS                     |
| Strategy              | Behavioral  | `InjectionToken` + swappable services               |
| Command + Undo/Redo   | Behavioral  | State management actions                            |
| Chain of Responsibility | Behavioral | HTTP Interceptors, middleware pipelines            |
| Repository            | Architectural | Data access abstraction layer                    |
| State Service         | Architectural | `BehaviorSubject` + derived streams              |

---

## Module 12 — Checkpoint Tasks

- [ ] Implement the full **Command + Undo/Redo** pattern for a text editor component.
- [ ] Build a **State Service** for product management using `BehaviorSubject` with `vm$` viewmodel stream.
- [ ] Create a **Builder** for constructing complex Angular `FormGroup` configurations.
- [ ] Implement a `Chain of Responsibility` for request validation — auth, rate limit, schema validation.
- [ ] Write an **HTTP Interceptor** that adds retry logic, auth headers, and global error handling.
- [ ] Design a **Repository** pattern for `UserRepository` with both HTTP and localStorage implementations, swappable via DI.

---

> **Module 12 complete.** Type `continue` to proceed to **Module 13 – Real-World Angular-Specific JavaScript Usage.**

---

# Module 13 – Real-World Angular-Specific JavaScript Usage

---

## 13.1 How Angular Uses JavaScript Under the Hood

### Simple Explanation
Angular is a TypeScript framework that compiles to optimized JavaScript. Every Angular concept — components, services, change detection, DI — is JavaScript patterns you already know, given structure.

### The Compilation Pipeline
```
TypeScript source
      ↓  tsc (type-check)
   Angular Compiler (ngc / Ivy)
      ↓  generates: component factories, injector trees
   esbuild / Webpack
      ↓  bundles + tree-shakes
   Optimised JavaScript
      ↓  runs in browser
```

### Angular Ivy — Function-Based Compilation
```ts
// What you write:
@Component({
  selector: 'app-user',
  template: `<h1>{{ user.name }}</h1>`
})
export class UserComponent {
  @Input() user!: User;
}

// What Ivy compiles to (simplified):
UserComponent.ɵcmp = defineComponent({
  type: UserComponent,
  selectors: [['app-user']],
  inputs: { user: 'user' },
  decls: 2,
  vars: 1,
  template: function(rf, ctx) {
    if (rf & 1) {  // CREATE phase
      elementStart(0, 'h1');
      text(1);
      elementEnd();
    }
    if (rf & 2) {  // UPDATE phase — only runs when dirty
      textInterpolate(2, ctx.user.name);
    }
  }
});
```

---

## 13.2 Decorators — The Metadata System

### What Decorators Really Do
```ts
// @Component is just a function that receives the class
// and attaches metadata to it
function Component(metadata: ComponentMetadata) {
  return function(target: Function) {
    // Store metadata on the class
    Reflect.defineMetadata('component', metadata, target);
    // Angular's injector reads this at bootstrap time
  };
}

// @Injectable marks a class for DI
function Injectable(options = {}) {
  return function(target: Function) {
    Reflect.defineMetadata('injectable', options, target);
  };
}

// Parameter decorators — mark constructor params for injection
function Inject(token: any) {
  return function(target: Function, _: string, paramIndex: number) {
    const params = Reflect.getMetadata('design:paramtypes', target) || [];
    params[paramIndex] = token;
    Reflect.defineMetadata('design:paramtypes', params, target);
  };
}
```

### Common Angular Decorators — Deep Reference
```ts
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `...`,
  styles: [`...`],
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.Emulated,
  animations: [fadeInOut],
  providers: [{ provide: CacheService, useClass: DashboardCacheService }]
})

@Directive({ selector: '[appHighlight]', standalone: true })

@Pipe({ name: 'truncate', pure: true, standalone: true })

@Injectable({
  providedIn: 'root',            // singleton app-wide
  // providedIn: 'any'           // new instance per lazy-loaded module
  // providedIn: UserModule      // scoped to UserModule
})

// Property decorators
@Input()  user!: User;
@Input({ required: true }) config!: Config;
@Input({ alias: 'srcUrl' }) url!: string;

@Output() userChange = new EventEmitter<User>();

@ViewChild('nameInput')   nameInput!: ElementRef<HTMLInputElement>;
@ViewChild(DialogComponent) dialog!: DialogComponent;
@ViewChildren(CardComponent) cards!: QueryList<CardComponent>;

@ContentChild(HeaderComponent)   header!: HeaderComponent;
@ContentChildren(TabComponent)   tabs!: QueryList<TabComponent>;

@HostBinding('class.active') isActive = false;
@HostBinding('attr.aria-label') label = 'User card';

@HostListener('click', ['$event'])
onClick(event: MouseEvent) { this.isActive = !this.isActive; }
```

---

## 13.3 Change Detection — Deep Dive

### The Change Detection Tree
```
AppComponent
├── NavComponent         (Default CD)
├── DashboardComponent   (OnPush — only checks on new Input refs or Events)
│   ├── ChartComponent   (OnPush)
│   └── TableComponent   (OnPush)
│       └── RowComponent (OnPush)
└── SidebarComponent     (Default CD)
```

### How Zone.js Triggers Change Detection
```ts
// Zone.js monkey-patches async APIs to notify Angular
// Every time these fire, Angular runs change detection from root:
// - setTimeout / setInterval
// - Promise.then
// - XMLHttpRequest / fetch
// - DOM events

// This is why Angular "just works" without manual update calls
button.addEventListener('click', () => {
  this.count++; // Angular detects this automatically via Zone.js
});

// Running outside zone — no automatic CD
this.zone.runOutsideAngular(() => {
  requestAnimationFrame(() => {
    this.canvas.drawFrame(); // no CD triggered — performance win
  });
});
```

### Manual Change Detection
```ts
@Component({ changeDetection: ChangeDetectionStrategy.OnPush })
export class DataTableComponent {
  data: Row[] = [];

  constructor(private cdr: ChangeDetectorRef) {}

  // Force check this component and descendants
  refresh() {
    this.cdr.markForCheck();      // schedule check on next CD cycle
    // or:
    this.cdr.detectChanges();     // immediately run CD for this subtree
  }

  // Detach from CD tree entirely — manual control
  ngOnInit() {
    this.cdr.detach();
    // Now ONLY detectChanges() will update this component
  }
}
```

### Signal-Based Change Detection (Angular 17+)
```ts
@Component({
  template: `
    <p>{{ count() }}</p>
    <p>{{ doubled() }}</p>
    <button (click)="increment()">+</button>
  `
})
export class CounterComponent {
  count   = signal(0);
  doubled = computed(() => this.count() * 2);

  // Signal mutation functions
  increment() { this.count.update(n => n + 1); }
  reset()      { this.count.set(0); }
  add(n: number) { this.count.update(c => c + n); }

  // effect — runs when ANY signal it reads changes
  logEffect = effect(() => {
    console.log(`Count is now: ${this.count()}`);
    // Automatically tracks count() as dependency
  });

  // Input signals (Angular 17.1+)
  user = input<User>();       // optional signal input
  name = input.required<string>(); // required signal input
}
```

---

## 13.4 RxJS — Real-World Patterns

### The Essential Operators
```ts
import {
  map, filter, switchMap, mergeMap, concatMap, exhaustMap,
  debounceTime, distinctUntilChanged, catchError, retry,
  takeUntil, takeUntilDestroyed, tap, share, shareReplay,
  combineLatest, forkJoin, merge, startWith, scan, reduce,
  withLatestFrom, zip, race, of, from, fromEvent, interval,
  timer, EMPTY, throwError, defer
} from 'rxjs';
```

### SwitchMap vs MergeMap vs ConcatMap vs ExhaustMap
```ts
// switchMap — cancel previous, use latest (search, route params)
searchControl.valueChanges.pipe(
  debounceTime(300),
  switchMap(term => this.api.search(term))  // cancels in-flight request on new term
);

// mergeMap — run all, don't cancel (parallel, independent tasks)
ids$.pipe(
  mergeMap(id => this.api.deleteUser(id))  // all deletions run in parallel
);

// concatMap — queue, run in order (sequential operations)
saves$.pipe(
  concatMap(data => this.api.save(data))   // wait for each save before next
);

// exhaustMap — ignore new while busy (button spam prevention)
buttonClicks$.pipe(
  exhaustMap(() => this.api.submitForm())  // ignores clicks while submitting
);
```

### The Ultimate ViewModel Pattern
```ts
@Component({
  template: `
    <ng-container *ngIf="vm$ | async as vm">
      <div *ngIf="vm.loading">Loading...</div>
      <div *ngIf="vm.error">{{ vm.error }}</div>
      <div *ngFor="let user of vm.users; trackBy: trackById">
        {{ user.name }} ({{ vm.selectedCount }} selected)
      </div>
    </ng-container>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserListComponent {
  private searchTerm$ = new BehaviorSubject('');
  private refresh$    = new Subject<void>();

  readonly vm$ = this.refresh$.pipe(
    startWith(null),
    switchMap(() => this.userService.getAll().pipe(
      map(users => ({ users, loading: false, error: null })),
      startWith({ users: [] as User[], loading: true, error: null }),
      catchError(err => of({ users: [] as User[], loading: false, error: err.message }))
    )),
    combineLatestWith(this.searchTerm$),
    map(([state, term]) => ({
      ...state,
      users: state.users.filter(u =>
        u.name.toLowerCase().includes(term.toLowerCase())
      ),
      selectedCount: state.users.filter(u => u.selected).length
    })),
    shareReplay(1)
  );

  search(term: string) { this.searchTerm$.next(term); }
  refresh()            { this.refresh$.next(); }
  trackById = (_: number, u: User) => u.id;
}
```

---

## 13.5 Forms — Reactive vs Template-Driven

### Reactive Forms (JavaScript-centric — preferred)
```ts
@Component({ ... })
export class RegistrationComponent {
  form = new FormGroup({
    // Basic controls
    name:     new FormControl('', [Validators.required, Validators.minLength(2)]),
    email:    new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/(?=.*[A-Z])(?=.*[0-9])/)
    ]),
    // Nested group
    address: new FormGroup({
      street: new FormControl(''),
      city:   new FormControl('', Validators.required),
    }),
    // Dynamic array
    phones: new FormArray([]),
  });

  // Typed form (Angular 14+)
  typedForm = new FormGroup({
    name:  new FormControl<string>('',  { nonNullable: true }),
    age:   new FormControl<number>(18,  { nonNullable: true }),
    email: new FormControl<string | null>(null),
  });

  addPhone() {
    this.phones.push(new FormControl('', Validators.pattern(/^\d{10}$/)));
  }

  get phones() { return this.form.get('phones') as FormArray; }
  get nameError() {
    const ctrl = this.form.get('name')!;
    if (ctrl.hasError('required')) return 'Name is required';
    if (ctrl.hasError('minlength')) return 'At least 2 characters';
    return null;
  }

  // Cross-field validator
  passwordMatch: ValidatorFn = (group: AbstractControl) => {
    const pass    = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return pass === confirm ? null : { passwordMismatch: true };
  };

  // Async validator
  uniqueEmail: AsyncValidatorFn = (ctrl: AbstractControl) =>
    this.userService.checkEmail(ctrl.value).pipe(
      map(exists => exists ? { emailTaken: true } : null),
      catchError(() => of(null))
    );

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched(); // show all errors
      return;
    }
    const data = this.form.getRawValue(); // includes disabled controls
    this.userService.register(data).subscribe();
  }
}
```

---

## 13.6 Routing — JavaScript Patterns

```ts
// Route guards using inject()
export const authGuard: CanActivateFn = (route, state) => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  return auth.isLoggedIn()
    ? true
    : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

// Resolve guard — fetch data before component activates
export const userResolver: ResolveFn<User> = (route) => {
  const userService = inject(UserService);
  const router      = inject(Router);
  return userService.getById(+route.paramMap.get('id')!).pipe(
    catchError(() => {
      router.navigate(['/not-found']);
      return EMPTY;
    })
  );
};

const routes: Routes = [
  {
    path: 'users/:id',
    component: UserDetailComponent,
    canActivate: [authGuard],
    resolve: { user: userResolver },
    data: { title: 'User Detail', breadcrumb: 'User' }
  },
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.routes'),
    canMatch: [() => inject(AuthService).hasRole('admin')],
  }
];

// In component — consuming resolved data
export class UserDetailComponent {
  user = inject(ActivatedRoute).snapshot.data['user'] as User;

  // Or reactively
  user$ = inject(ActivatedRoute).data.pipe(map(d => d['user'] as User));
}
```

---

## 13.7 HTTP — Advanced Patterns

```ts
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = inject(API_URL);

  // Generic CRUD with full type safety
  get<T>(path: string, params?: Record<string, any>): Observable<T> {
    return this.http.get<T>(`${this.baseUrl}${path}`, { params });
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(`${this.baseUrl}${path}`, body);
  }

  // Pagination helper
  getPaginated<T>(path: string, page: number, size = 20): Observable<Page<T>> {
    return this.http.get<Page<T>>(`${this.baseUrl}${path}`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  // Upload with progress
  upload(file: File): Observable<number | string> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post('/api/upload', formData, {
      reportProgress: true,
      observe: 'events'
    }).pipe(
      map(event => {
        if (event.type === HttpEventType.UploadProgress) {
          return Math.round(100 * event.loaded / (event.total ?? 1));
        }
        if (event.type === HttpEventType.Response) return event.body as string;
        return 0;
      })
    );
  }
}

// Caching interceptor
@Injectable()
export class CacheInterceptor implements HttpInterceptor {
  private cache = new Map<string, HttpResponse<any>>();

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    if (req.method !== 'GET') return next.handle(req);

    const cached = this.cache.get(req.urlWithParams);
    if (cached) return of(cached.clone());

    return next.handle(req).pipe(
      tap(event => {
        if (event instanceof HttpResponse) {
          this.cache.set(req.urlWithParams, event.clone());
        }
      })
    );
  }
}
```

---

## 13.8 Angular Animations — JavaScript API

```ts
import {
  trigger, state, style, animate, transition,
  keyframes, stagger, query, animateChild, group, sequence
} from '@angular/animations';

@Component({
  animations: [
    // Fade in/out
    trigger('fadeInOut', [
      state('void', style({ opacity: 0, transform: 'translateY(-10px)' })),
      transition(':enter', [animate('200ms ease-out')]),
      transition(':leave', [animate('150ms ease-in',
        style({ opacity: 0, transform: 'translateY(-10px)' }))
      ])
    ]),

    // Loading skeleton pulse
    trigger('pulse', [
      transition('* <=> *', [
        animate('1s ease-in-out', keyframes([
          style({ opacity: 0.4, offset: 0   }),
          style({ opacity: 1.0, offset: 0.5 }),
          style({ opacity: 0.4, offset: 1.0 }),
        ]))
      ])
    ]),

    // Stagger list items
    trigger('listAnimation', [
      transition('* => *', [
        query(':enter', [
          style({ opacity: 0, transform: 'translateX(-20px)' }),
          stagger('50ms', [
            animate('200ms ease-out',
              style({ opacity: 1, transform: 'none' }))
          ])
        ], { optional: true })
      ])
    ])
  ]
})
export class AnimatedListComponent {
  @HostBinding('@listAnimation') get animationState() { return this.items.length; }
}
```

---

## 13.9 Angular Signals — Complete Patterns

```ts
@Injectable({ providedIn: 'root' })
export class UserStore {
  // State as signals
  private _users    = signal<User[]>([]);
  private _loading  = signal(false);
  private _error    = signal<string | null>(null);
  private _selected = signal<number | null>(null);

  // Derived (computed) state
  readonly users         = this._users.asReadonly();
  readonly loading       = this._loading.asReadonly();
  readonly error         = this._error.asReadonly();
  readonly activeUsers   = computed(() => this._users().filter(u => u.active));
  readonly selectedUser  = computed(() =>
    this._users().find(u => u.id === this._selected()) ?? null
  );
  readonly stats = computed(() => ({
    total:  this._users().length,
    active: this.activeUsers().length,
    admins: this._users().filter(u => u.role === 'admin').length,
  }));

  constructor(private http: HttpClient) {
    // effect — run side effects when signals change
    effect(() => {
      const user = this.selectedUser();
      if (user) console.log('Selected user changed:', user.name);
    });
  }

  loadUsers = rxResource({
    loader: () => this.http.get<User[]>('/api/users'),
  });

  selectUser(id: number) { this._selected.set(id); }

  addUser(user: User) {
    this._users.update(users => [...users, user]);
  }

  removeUser(id: number) {
    this._users.update(users => users.filter(u => u.id !== id));
  }

  updateUser(id: number, changes: Partial<User>) {
    this._users.update(users =>
      users.map(u => u.id === id ? { ...u, ...changes } : u)
    );
  }
}

// Component consuming the store
@Component({
  template: `
    @if (store.loading()) { <app-spinner /> }
    @if (store.error()) { <app-error [message]="store.error()!" /> }

    @for (user of store.activeUsers(); track user.id) {
      <app-user-card
        [user]="user"
        [selected]="store.selectedUser()?.id === user.id"
        (click)="store.selectUser(user.id)"
      />
    }

    <p>Stats: {{ store.stats() | json }}</p>
  `
})
export class UserListComponent {
  store = inject(UserStore);
}
```

---

## 13.10 Testing JavaScript Logic in Angular

```ts
// Unit test pure functions — no Angular needed
describe('calculateDiscount', () => {
  it('applies seasonal discount', () => {
    expect(calculateDiscount(100, 'seasonal')).toBe(90);
  });
  it('applies vip discount', () => {
    expect(calculateDiscount(100, 'vip')).toBe(80);
  });
});

// Component testing — focused, fast
describe('UserCardComponent', () => {
  let component: UserCardComponent;
  let fixture: ComponentFixture<UserCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserCardComponent],  // standalone component
      providers: [
        { provide: UserService, useValue: mockUserService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserCardComponent);
    component = fixture.componentInstance;
    component.user = mockUser;
    fixture.detectChanges();
  });

  it('renders user name', () => {
    const h2 = fixture.nativeElement.querySelector('h2');
    expect(h2.textContent).toContain('Alice');
  });

  it('emits userSelected on click', () => {
    let emittedUser: User | undefined;
    component.userSelected.subscribe(u => emittedUser = u);
    fixture.nativeElement.querySelector('button').click();
    expect(emittedUser).toEqual(mockUser);
  });
});

// Service testing with Observable
describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserService]
    });
    service   = TestBed.inject(UserService);
    httpMock  = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify()); // ensure no unexpected requests

  it('fetches users', (done) => {
    service.getAll().subscribe(users => {
      expect(users).toHaveLength(2);
      done();
    });
    const req = httpMock.expectOne('/api/users');
    expect(req.request.method).toBe('GET');
    req.flush([mockUser1, mockUser2]);
  });
});
```

---

## Module 13 — Summary Notes

| Concept                     | Key Takeaway                                                              |
|-----------------------------|--------------------------------------------------------------------------|
| Ivy compilation             | Splits template into CREATE (rf & 1) + UPDATE (rf & 2) phases           |
| Decorators                  | Functions that attach metadata — Angular reads at bootstrap              |
| Zone.js                     | Monkey-patches async APIs to trigger CD automatically                    |
| OnPush + Signals            | Fine-grained reactivity — future of Angular CD                          |
| switchMap                   | Cancel previous (search, route); exhaustMap = ignore while busy (submit) |
| ViewModel pattern (`vm$`)   | Single `async` pipe delivering all template data                        |
| Typed Reactive Forms        | `FormControl<T>`, `nonNullable`, `getRawValue()`                        |
| Functional guards/resolvers | `inject()` in guard functions — clean, testable, no class boilerplate   |
| Signals store               | `signal` + `computed` + `effect` = reactive state without RxJS          |
| Testing                     | Pure functions need no Angular; components use TestBed with mocks       |

---

## Module 13 — Checkpoint Tasks

- [ ] Build a full **ViewModel pattern** component with search, pagination, loading, error state — all via a single `vm$` observable.
- [ ] Create a **Signals-based store** for a shopping cart: add, remove, update quantity, computed subtotal + tax + total.
- [ ] Implement a **typed reactive form** for user registration with async email uniqueness validator.
- [ ] Write a **caching HTTP interceptor** that stores GET responses and serves cache for 60 seconds.
- [ ] Build an animated list with **enter/leave stagger animations** using Angular Animations API.
- [ ] Write complete **unit + integration tests** for a service and its consuming component using mocks and `HttpTestingController`.

---

> **Module 13 complete.** Type `continue` to proceed to **Module 14 – Interview Preparation.**

---

# Module 14 – Interview Preparation

---

## 14.1 How to Approach JavaScript Interviews

### The Framework
1. **Clarify** — repeat the question back, ask about constraints.
2. **Think aloud** — narrate your reasoning, interviewers value process.
3. **State the naive solution** — then optimise.
4. **Identify edge cases** — null, empty, large input, duplicates.
5. **Analyse complexity** — time (O notation) and space.
6. **Test** — walk through your code with an example.

### What Senior Roles Test
| Level    | Focus                                                              |
|----------|--------------------------------------------------------------------|
| Junior   | Syntax, basic algorithms, simple async                            |
| Mid      | Closures, prototypes, promises, event loop, OOP                   |
| Senior   | System design, architecture, performance, patterns, deep JS       |
| Angular  | Change detection, RxJS, DI, lazy loading, signals, testing        |

---

## 14.2 Core JavaScript — Top 50 Interview Questions

### Variables & Types

**Q1: What is the difference between `var`, `let`, and `const`?**
> `var` is function-scoped and hoisted as `undefined`. `let` and `const` are block-scoped with TDZ. `const` prevents reassignment of the binding but not mutation of the value. Always prefer `const`, then `let`, never `var`.

**Q2: What is the Temporal Dead Zone?**
> The period between entering a block scope and the `let`/`const` declaration being initialised. Accessing the variable in this zone throws a `ReferenceError`.

**Q3: Why is `typeof null === 'object'`?**
> A historic bug in the first JavaScript implementation. The binary representation of `null` was all zeros, which matched the object type tag. It was never fixed for backward compatibility.

**Q4: What is the difference between `==` and `===`?**
> `==` performs type coercion before comparison. `===` compares value AND type without coercion. Always use `===`.

**Q5: What are all the falsy values in JavaScript?**
> `false`, `0`, `-0`, `0n`, `''` (empty string), `null`, `undefined`, `NaN`. Everything else is truthy — including `[]`, `{}`, `'0'`, `-1`.

**Q6: What is the difference between `null` and `undefined`?**
> `undefined` is the default value of uninitialized variables/missing function arguments/missing object properties. `null` is an explicit, intentional absence of value set by the programmer.

---

### Functions & Scope

**Q7: What is a closure?**
> A closure is a function that retains access to its lexical scope even when executed outside that scope. It closes over the variables in its outer environment — holding a live reference, not a copy.
```js
function makeCounter() {
  let n = 0;
  return () => ++n;
}
const c = makeCounter();
c(); // 1  — n is remembered
c(); // 2
```

**Q8: What does `this` refer to in different contexts?**
> 1. Global context: `window` (browser) / `global` (Node) / `undefined` in strict mode.
> 2. Object method: the object before the dot.
> 3. Constructor (`new`): the newly created object.
> 4. `call`/`apply`/`bind`: the explicitly passed object.
> 5. Arrow function: inherits `this` from the enclosing lexical scope — never has its own.

**Q9: What is the difference between `call`, `apply`, and `bind`?**
> All three set `this` explicitly. `call(ctx, a, b)` invokes immediately with individual args. `apply(ctx, [a, b])` invokes immediately with args as an array. `bind(ctx, a)` returns a new function with `this` permanently bound — called later.

**Q10: Explain hoisting.**
> During the creation phase, the JS engine scans the scope and — function declarations are fully loaded; `var` declarations are set to `undefined`; `let`/`const`/`class` are registered but in TDZ. Execution happens after.

**Q11: What is the difference between lexical and dynamic scope?**
> JavaScript uses lexical scope — where a function is **defined** determines its scope chain, not where it is **called**. `this` is the exception; it uses dynamic binding (determined at call time). Arrow functions restore lexical `this`.

**Q12: What are the differences between arrow functions and regular functions?**
> 1. No own `this` — inherits from enclosing scope.
> 2. No `arguments` object — use rest params.
> 3. Cannot be used with `new`.
> 4. No `prototype` property.
> 5. Implicit return for single expressions.
> 6. Not hoisted.

---

### Prototypes & OOP

**Q13: How does prototypal inheritance work?**
> Every object has an internal `[[Prototype]]` link. When you access a property, JS looks in the object first, then walks up the chain to `Object.prototype`, then `null`. This chain is set up via `new`, `Object.create()`, or `class extends`.

**Q14: What does the `new` keyword do?**
> 1. Creates a new empty object.
> 2. Sets its `[[Prototype]]` to `Constructor.prototype`.
> 3. Calls the constructor with `this` = that new object.
> 4. Returns the new object (unless constructor explicitly returns a different object).

**Q15: What is the difference between `__proto__` and `prototype`?**
> `prototype` is a property of **constructor functions/classes** — it's the object that instances will inherit from. `__proto__` (or `Object.getPrototypeOf()`) is the internal `[[Prototype]]` of an **instance** — it points to its constructor's `prototype`.
```js
function Dog() {}
const rex = new Dog();
rex.__proto__ === Dog.prototype;  // true
Dog.prototype.__proto__ === Object.prototype; // true
```

**Q16: What is the difference between composition and inheritance? Which is preferred?**
> Inheritance is an "is-a" relationship via prototype chain. Composition is a "has-a" relationship — building objects by combining behaviour. Composition is preferred: more flexible, no tight coupling, avoids fragile base class problems. "Favour composition over inheritance" — GoF.

---

### Async JavaScript

**Q17: What is the Event Loop?**
> The mechanism that allows JS to be non-blocking despite being single-threaded. When the call stack is empty, the event loop picks the next task from the queue. Microtasks (Promises) are processed entirely before the next macrotask (setTimeout).

**Q18: What is the difference between microtasks and macrotasks?**
> Microtasks: `Promise.then`, `queueMicrotask`, `MutationObserver`. Run after the current task, before the next macrotask.
> Macrotasks: `setTimeout`, `setInterval`, I/O, `requestAnimationFrame`. One per event loop tick.

**Q19: What is the output?**
```js
console.log('1');
setTimeout(() => console.log('2'), 0);
Promise.resolve().then(() => console.log('3'));
console.log('4');
```
> Output: `1`, `4`, `3`, `2`. Sync runs first → microtask (Promise) before macrotask (setTimeout).

**Q20: What are the differences between `Promise.all`, `Promise.allSettled`, `Promise.race`, and `Promise.any`?**
> - `all` — resolves when ALL resolve; rejects on FIRST rejection.
> - `allSettled` — waits for ALL; returns array of `{status, value/reason}`.
> - `race` — settles with the FIRST settled (fulfilled or rejected).
> - `any` — resolves with FIRST fulfilled; rejects only if ALL reject.

**Q21: What is `async/await` and how does it relate to Promises?**
> Syntactic sugar over Promises. An `async` function always returns a Promise. `await` pauses execution of the async function until the Promise settles — other code continues running. Errors must be caught with `try/catch`.

**Q22: What are common async mistakes?**
> 1. Forgetting `await` — gets Promise instead of value.
> 2. Not handling rejections.
> 3. Sequential `await` in a loop instead of `Promise.all` (slow).
> 4. Using `forEach` with async — it doesn't wait for Promises.
> 5. Swallowing errors in empty `catch` blocks.

---

### Arrays & Objects

**Q23: What is the difference between `map`, `filter`, `reduce`, and `forEach`?**
> - `map` — transforms each element, returns new array same length.
> - `filter` — selects matching elements, returns new shorter array.
> - `reduce` — accumulates to a single value.
> - `forEach` — iterates for side effects, returns `undefined` — not chainable.

**Q24: How do you deep clone an object?**
> 1. `structuredClone(obj)` — modern, handles dates, sets, maps, but not functions.
> 2. `JSON.parse(JSON.stringify(obj))` — only for JSON-safe data; drops functions/undefined/symbols.
> 3. Custom recursive traversal for full control.

**Q25: What is the difference between shallow and deep copy?**
> Shallow copy (spread `{...}`, `Object.assign`) copies one level — nested objects are still shared references. Deep copy duplicates the entire nested structure — no shared references.

**Q26: How does `Array.sort()` work by default and why is it a problem for numbers?**
> Default sort converts elements to strings and sorts lexicographically. `[10, 9, 2].sort()` → `[10, 2, 9]` because `'10' < '2'` as strings. Always provide a comparator: `.sort((a, b) => a - b)`.

---

### Modules & Patterns

**Q27: What is the difference between CommonJS and ES Modules?**
> CommonJS: `require/module.exports`, synchronous, no tree-shaking, Node.js legacy.
> ESM: `import/export`, asynchronous, live bindings, tree-shakeable, modern standard.

**Q28: What is tree shaking?**
> The bundler's ability to eliminate dead (unused) code from the final bundle. Only works with ES modules (static `import/export`) because CJS `require()` is dynamic and cannot be statically analysed.

**Q29: What is a closure-based module pattern?**
> An IIFE that returns a public API while keeping private variables inaccessible from the outside. The predecessor to ES modules.

---

### Performance

**Q30: What is layout thrashing?**
> Alternating DOM reads (that force layout recalculation) and writes (that invalidate layout) in a loop. Fix by batching all reads first, then all writes.

**Q31: What is debounce vs throttle?**
> Debounce: execute only after N ms of inactivity — good for search input.
> Throttle: execute at most once per N ms — good for scroll/resize.

**Q32: What is memoization?**
> Caching a function's return value based on its arguments. On repeated calls with the same args, return the cached result instead of recomputing.

---

## 14.3 Angular Interview Questions — Top 30

**Q1: What is the difference between `Default` and `OnPush` change detection?**
> Default: Angular checks the entire component tree on every CD cycle (after any async event). OnPush: Angular only checks when an `@Input` reference changes, an Observable via `async` pipe emits, or an event fires in the component. OnPush requires immutable data patterns but is far more performant.

**Q2: What is Zone.js and why does Angular use it?**
> Zone.js monkey-patches browser async APIs (setTimeout, Promises, DOM events) to create an execution context. Angular's `NgZone` hooks into this to automatically trigger change detection after every async operation.

**Q3: What is the difference between `Subject`, `BehaviorSubject`, and `ReplaySubject`?**
> `Subject`: no initial value, only future emissions to current subscribers.
> `BehaviorSubject`: stores current value, new subscribers get it immediately. Has synchronous `.getValue()`.
> `ReplaySubject(n)`: replays last N emissions to new subscribers.

**Q4: What is the difference between `switchMap`, `mergeMap`, `concatMap`, and `exhaustMap`?**
> switchMap: cancels previous inner observable — search, route params.
> mergeMap: runs all concurrently — parallel independent tasks.
> concatMap: queues, runs in order — sequential operations.
> exhaustMap: ignores new while busy — button spam, login submit.

**Q5: How does Angular's DI system work?**
> Angular builds an injector tree matching the component tree. When a component needs a dependency, Angular walks up the injector hierarchy looking for a provider. `providedIn: 'root'` creates a singleton in the root injector. Component-level providers create a new instance per component.

**Q6: What is the difference between `ViewChild` and `ContentChild`?**
> `ViewChild` queries elements/components in the component's own template. `ContentChild` queries content projected via `<ng-content>` from the parent. Available after `ngAfterViewInit` and `ngAfterContentInit` respectively.

**Q7: What is the Angular lifecycle and what happens in each hook?**
> 1. `ngOnChanges` — `@Input` changed (before OnInit)
> 2. `ngOnInit` — component initialised, inputs available
> 3. `ngDoCheck` — custom CD
> 4. `ngAfterContentInit` — projected content initialised
> 5. `ngAfterContentChecked`
> 6. `ngAfterViewInit` — component's view + child views initialised
> 7. `ngAfterViewChecked`
> 8. `ngOnDestroy` — cleanup subscriptions, listeners, timers

**Q8: What is a pure vs impure pipe?**
> Pure pipe: called only when the input reference changes — like a pure function. Angular caches the result. Default.
> Impure pipe: called on every CD cycle — use only when internal state of the input changes (e.g., async or mutable data). Declare with `pure: false`.

**Q9: What is the async pipe and why is it preferred?**
> The `async` pipe subscribes to an Observable/Promise and unwraps its latest value. It automatically unsubscribes when the component is destroyed, triggering change detection on each emission. No manual `subscribe` or `ngOnDestroy` needed.

**Q10: What are Angular Signals and how do they differ from RxJS?**
> Signals are synchronous, fine-grained reactive primitives. `signal(value)` stores a value; `computed()` derives values; `effect()` runs side effects. Unlike RxJS Observables (async, stream-based), Signals always have a current value, are synchronous, and enable fine-grained DOM updates without Zone.js.

**Q11: What is lazy loading and how does it work in Angular?**
> Lazy loading defers loading of a feature module/component until its route is first navigated to. In the router, `loadChildren` or `loadComponent` returns a dynamic `import()`. The bundler splits that code into a separate chunk downloaded on demand.

**Q12: What is the difference between template-driven and reactive forms?**
> Template-driven: form logic in the template, implicit `NgModel` binding, async validation difficult, less testable.
> Reactive: form logic in the component class, explicit `FormControl`/`FormGroup`, fully synchronous and testable, better for complex forms.

**Q13: How do you prevent memory leaks in Angular?**
> 1. Use `async` pipe instead of manual subscribe.
> 2. Use `takeUntilDestroyed()` (Angular 16+) for component-scoped subscriptions.
> 3. Clear `setInterval`/`setTimeout` in `ngOnDestroy`.
> 4. Remove DOM `addEventListener` in `ngOnDestroy`.
> 5. Use `DestroyRef` for cleanup callbacks.

**Q14: What is `trackBy` and why is it important?**
> `trackBy` provides a function to `*ngFor` that uniquely identifies items. Without it, Angular destroys and recreates all DOM nodes when the array reference changes. With it, Angular only patches changed items — critical for lists > 20 items.

**Q15: What is the difference between `providedIn: 'root'` and providing in a component?**
> `providedIn: 'root'`: singleton for the entire app, tree-shaken if unused.
> Component providers: new instance per component instance, destroyed with the component. Use for stateful services that should not be shared globally.

---

## 14.4 Coding Challenges — Classic Interview Problems

### Challenge 1: Implement `debounce`
```js
function debounce(fn, delay) {
  let timer;
  return function(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}
```

### Challenge 2: Implement `flatten`
```js
// Flatten nested array to any depth
function flatten(arr, depth = Infinity) {
  return depth > 0
    ? arr.reduce((acc, val) =>
        Array.isArray(val)
          ? acc.concat(flatten(val, depth - 1))
          : acc.concat(val),
        [])
    : arr.slice();
}
flatten([1, [2, [3, [4]]]]);        // [1, 2, 3, 4]
flatten([1, [2, [3, [4]]]], 1);     // [1, 2, [3, [4]]]
```

### Challenge 3: Implement `deepEqual`
```js
function deepEqual(a, b) {
  if (a === b) return true;
  if (a === null || b === null) return false;
  if (typeof a !== 'object' || typeof b !== 'object') return false;
  if (Array.isArray(a) !== Array.isArray(b)) return false;
  const keysA = Object.keys(a);
  const keysB = Object.keys(b);
  if (keysA.length !== keysB.length) return false;
  return keysA.every(key => deepEqual(a[key], b[key]));
}
```

### Challenge 4: Implement `Promise.all`
```js
function promiseAll(promises) {
  return new Promise((resolve, reject) => {
    if (!promises.length) return resolve([]);
    const results = [];
    let remaining = promises.length;
    promises.forEach((p, i) => {
      Promise.resolve(p).then(val => {
        results[i] = val;
        if (--remaining === 0) resolve(results);
      }).catch(reject);
    });
  });
}
```

### Challenge 5: Implement `curry`
```js
function curry(fn) {
  return function curried(...args) {
    if (args.length >= fn.length) return fn(...args);
    return (...more) => curried(...args, ...more);
  };
}
```

### Challenge 6: Implement `pipe`
```js
const pipe = (...fns) => x => fns.reduce((v, f) => f(v), x);
```

### Challenge 7: Implement `groupBy`
```js
function groupBy(arr, keyFn) {
  return arr.reduce((groups, item) => {
    const key = keyFn(item);
    (groups[key] ??= []).push(item);
    return groups;
  }, {});
}
groupBy([{ role: 'admin', name: 'Alice' }, { role: 'user', name: 'Bob' }], u => u.role);
// { admin: [Alice], user: [Bob] }
```

### Challenge 8: LRU Cache
```js
class LRUCache {
  #cache;
  #maxSize;

  constructor(maxSize) {
    this.#cache   = new Map();
    this.#maxSize = maxSize;
  }

  get(key) {
    if (!this.#cache.has(key)) return -1;
    const val = this.#cache.get(key);
    this.#cache.delete(key);
    this.#cache.set(key, val); // move to front (most recent)
    return val;
  }

  put(key, value) {
    if (this.#cache.has(key)) this.#cache.delete(key);
    else if (this.#cache.size >= this.#maxSize)
      this.#cache.delete(this.#cache.keys().next().value); // evict oldest
    this.#cache.set(key, value);
  }
}
```

### Challenge 9: Event Emitter
```js
class EventEmitter {
  #events = new Map();

  on(event, fn) {
    (this.#events.get(event) ?? this.#events.set(event, new Set()).get(event)).add(fn);
    return () => this.off(event, fn);
  }

  off(event, fn) { this.#events.get(event)?.delete(fn); }

  emit(event, ...args) { this.#events.get(event)?.forEach(fn => fn(...args)); }

  once(event, fn) {
    const wrapper = (...args) => { fn(...args); this.off(event, wrapper); };
    return this.on(event, wrapper);
  }
}
```

### Challenge 10: Implement `new`
```js
function myNew(Constructor, ...args) {
  const obj = Object.create(Constructor.prototype);
  const result = Constructor.apply(obj, args);
  return result instanceof Object ? result : obj;
}
```

---

## 14.5 Output Prediction Questions

```js
// Q1
console.log(typeof null);         // 'object'
console.log(typeof undefined);    // 'undefined'
console.log(null == undefined);   // true
console.log(null === undefined);  // false

// Q2
var x = 1;
function foo() {
  console.log(x);  // undefined (hoisting — local var x shadows)
  var x = 2;
  console.log(x);  // 2
}
foo();

// Q3
const obj = { a: 1 };
const copy = Object.assign({}, obj);
copy.a = 99;
console.log(obj.a); // 1 (primitive value copied)

const obj2 = { nested: { b: 2 } };
const copy2 = Object.assign({}, obj2);
copy2.nested.b = 99;
console.log(obj2.nested.b); // 99 (nested reference shared)

// Q4
function Person(name) { this.name = name; }
Person.prototype.greet = function() { return `Hi, ${this.name}`; };
const p = new Person('Alice');
console.log(p.greet());           // 'Hi, Alice'
console.log(p.hasOwnProperty('greet')); // false

// Q5 — classic closure + loop
for (var i = 0; i < 3; i++) {
  setTimeout(() => console.log(i), 0);
}
// 3, 3, 3  (all share same var i)

for (let j = 0; j < 3; j++) {
  setTimeout(() => console.log(j), 0);
}
// 0, 1, 2  (let is block-scoped per iteration)

// Q6 — event loop
async function main() {
  console.log('A');
  await Promise.resolve();
  console.log('B');
}
console.log('C');
main();
console.log('D');
// C, A, D, B
```

---

## 14.6 System Design Questions (Senior Angular)

**Q: Design a real-time dashboard that displays live user activity data.**
- Use WebSocket service (`WebSocketSubject` from RxJS) for live updates.
- `BehaviorSubject` to maintain current state; `scan` to accumulate events.
- `OnPush` + virtual scrolling for performance.
- Reconnect strategy: `retryWhen` + `timer` for exponential backoff.
- Separate concerns: `DataService` (stream) → `StateService` (accumulate) → Component (display).

**Q: Design a large form wizard with 5 steps, validation, and draft saving.**
- Single `FormGroup` with nested groups per step.
- `FormArray` for dynamic sections.
- Step validity gates navigation.
- `valueChanges.pipe(debounceTime(500), distinctUntilChanged())` for auto-save to `localStorage`.
- `AsyncValidators` for server-side uniqueness checks.
- `Router` guards prevent navigating away from dirty form without confirmation.

**Q: How would you optimise a slow Angular list with 10,000 items?**
1. `trackBy` to prevent full DOM rebuild.
2. `OnPush` change detection on all components.
3. CDK Virtual Scroll — render only visible items.
4. Pagination or infinite scroll — reduce initial dataset.
5. Server-side filtering/sorting — send less data.
6. Web Worker for client-side sorting/filtering.
7. Pure pipes for formatting.
8. `shareReplay(1)` on shared observables.

---

## 14.7 Behavioral Interview Answers — JavaScript Senior

**"Tell me about a performance problem you solved."**
> "We had a dashboard refreshing every 5 seconds causing jank. I profiled it with DevTools and found the component tree was fully checked each interval via Default CD. I switched to `OnPush`, ran the interval `runOutsideAngular`, and re-entered the zone only when data changed. This reduced CD cycles by 95% and eliminated the jank."

**"How do you handle memory leaks in Angular?"**
> "I systematically use `takeUntilDestroyed()` for all component-scoped subscriptions, prefer the `async` pipe for template subscriptions, use `DestroyRef` for cleanup callbacks, and always clear `setInterval` in `ngOnDestroy`. I also audit components with Chrome Memory panel for heap growth."

**"What's the hardest bug you debugged?"**
> Frame this around: symptoms → hypothesis → isolation → root cause → fix → prevention. Mention tools like DevTools, RxJS marble diagrams, or `tap()` for Observable debugging.

---

## Module 14 — Interview Preparation Checklist

### JavaScript Core
- [ ] Can explain and predict the output of all 6 output prediction questions above.
- [ ] Can implement `debounce`, `curry`, `pipe`, `flatten`, `deepEqual`, `Promise.all`, `LRU Cache` from scratch.
- [ ] Can explain prototype chain, closure, hoisting, event loop in plain English.
- [ ] Can identify every `this` context scenario.

### Angular Specific
- [ ] Can explain `OnPush` + `trackBy` + immutability — the performance trinity.
- [ ] Can explain `switchMap` vs `mergeMap` vs `concatMap` vs `exhaustMap` with use cases.
- [ ] Can design a `BehaviorSubject`-based state service from scratch.
- [ ] Can explain Angular's DI tree, injector hierarchy, and scoped providers.
- [ ] Can describe the full Angular lifecycle with timing of each hook.
- [ ] Can implement a reactive form with async validators and dynamic `FormArray`.

### System Design
- [ ] Can design a live data dashboard architecture.
- [ ] Can optimise a large list with all 8 techniques.
- [ ] Can explain the trade-offs of Signals vs RxJS.

---

> **Module 14 complete.** Type `continue` to proceed to **Module 15 – Mastery Projects.**

---

# Module 15 – Mastery Projects

> These five capstone projects synthesise every module. Each project includes: specification, architecture, key patterns used, implementation guide, and Angular-specific focus areas. Build them in order — each one builds on the previous.

---

## Project 1: Custom State Management Library

### Specification
Build a lightweight, type-safe state management library from scratch — similar to a minimal NgRx/Zustand — using only Angular Signals + TypeScript. No external state library allowed.

### Architecture
```
store/
├── store.ts          ← core Store class (signals + computed)
├── actions.ts        ← action creator factory
├── reducer.ts        ← pure reducer type + createReducer helper
├── effects.ts        ← effect runner (async side effects)
├── selectors.ts      ← memoized selector factory
└── devtools.ts       ← time-travel debug logger
```

### Core Implementation
```typescript
// store.ts
import { signal, computed, Signal } from '@angular/core';

type Reducer<S, A> = (state: S, action: A) => S;

export class Store<S, A extends { type: string }> {
  readonly #state;
  readonly #history: S[] = [];
  readonly #reducer: Reducer<S, A>;

  constructor(initialState: S, reducer: Reducer<S, A>) {
    this.#state   = signal<S>(initialState);
    this.#reducer = reducer;
  }

  dispatch(action: A): void {
    this.#history.push(this.#state());
    this.#state.update(s => this.#reducer(s, action));
    console.debug('[Store]', action.type, this.#state());
  }

  select<T>(selector: (state: S) => T): Signal<T> {
    return computed(() => selector(this.#state()));
  }

  /** Time-travel: rewind N steps */
  rewind(steps = 1): void {
    const target = this.#history.splice(-steps, steps)[0];
    if (target !== undefined) this.#state.set(target);
  }
}

// actions.ts
export function createAction<T extends string, P = void>(type: T) {
  return (payload: P) => ({ type, payload });
}

// reducer.ts
type ActionCreatorReturn<T> = T extends (...args: any[]) => infer R ? R : never;

export function createReducer<S, AC extends Record<string, (...args: any[]) => any>>(
  initialState: S,
  handlers: { [K in keyof AC]?: (state: S, action: ActionCreatorReturn<AC[K]>) => S }
) {
  return (state: S = initialState, action: { type: string; payload?: any }): S => {
    const handler = handlers[action.type as keyof AC];
    return handler ? handler(state, action as any) : state;
  };
}
```

```typescript
// usage example — counter feature
const increment = createAction<'increment', number>('increment');
const decrement = createAction<'decrement', number>('decrement');
const reset     = createAction<'reset'>('reset');

interface CounterState { count: number }

const counterReducer = createReducer<CounterState, any>(
  { count: 0 },
  {
    increment: (s, a) => ({ count: s.count + a.payload }),
    decrement: (s, a) => ({ count: s.count - a.payload }),
    reset:     ()     => ({ count: 0 }),
  }
);

// in a component
@Injectable({ providedIn: 'root' })
export class CounterStore extends Store<CounterState, any> {
  readonly count = this.select(s => s.count);
  readonly doubled = computed(() => this.count() * 2);

  constructor() { super({ count: 0 }, counterReducer); }

  increment(by = 1) { this.dispatch(increment(by)); }
  decrement(by = 1) { this.dispatch(decrement(by)); }
  reset()           { this.dispatch(reset(undefined)); }
}
```

### Selectors (Memoized)
```typescript
// selectors.ts
export function createSelector<S, R>(
  projector: (state: S) => R
): (state: S) => R {
  let lastInput: S;
  let lastResult: R;
  return (state: S): R => {
    if (state === lastInput) return lastResult;
    lastInput  = state;
    lastResult = projector(state);
    return lastResult;
  };
}
```

### Effects (Async Side Effects)
```typescript
// effects.ts
import { effect } from '@angular/core';

export function createEffect<T>(
  source: () => T,
  handler: (value: T) => void | Promise<void>
): void {
  effect(() => { handler(source()); });
}

// usage
createEffect(
  () => counterStore.count(),
  count => {
    if (count < 0) counterStore.reset();
    localStorage.setItem('count', String(count));
  }
);
```

### Patterns Used
- Signals + `computed` for reactive state graph
- Private class fields (`#`) for encapsulation
- Factory functions for type-safe action creators
- Memoization in selectors
- Command pattern (dispatch)
- Memento pattern (time-travel history)

### Extension Challenges
1. Add an `EntityAdapter` for normalised collections (like NgRx Entity).
2. Add a `Redux DevTools` browser extension integration via `window.__REDUX_DEVTOOLS_EXTENSION__`.
3. Add optimistic updates with rollback on error.

---

## Project 2: Real-Time Collaborative Todo App

### Specification
A real-time todo application where multiple users see live updates via WebSocket. Demonstrates async patterns, RxJS, OnPush performance, and Angular forms.

### Architecture
```
app/
├── core/
│   ├── ws.service.ts         ← WebSocket abstraction + reconnect
│   ├── todo.service.ts       ← state + WS integration
│   └── auth.service.ts       ← user identity
├── features/
│   ├── todo-list/            ← OnPush, virtual scroll, trackBy
│   ├── todo-form/            ← Reactive form + validation
│   └── presence/             ← who's online
└── shared/
    ├── pipes/                ← pure filter/sort pipes
    └── directives/           ← focus-trap, escape-close
```

### WebSocket Service
```typescript
@Injectable({ providedIn: 'root' })
export class WsService {
  readonly #url     = 'wss://api.example.com/todos';
  readonly #socket$ = new Subject<MessageEvent>();
  #ws!: WebSocket;

  readonly messages$ = this.#socket$.pipe(
    map(e => JSON.parse(e.data) as WsMessage),
    share()
  );

  connect(): void {
    this.#ws = new WebSocket(this.#url);
    this.#ws.onmessage = e => this.#socket$.next(e);
    this.#ws.onclose   = () =>
      timer(2000).subscribe(() => this.connect()); // reconnect
  }

  send<T>(message: T): void {
    this.#ws.send(JSON.stringify(message));
  }

  disconnect(): void { this.#ws.close(); }
}
```

### Todo State Service
```typescript
interface Todo { id: string; text: string; done: boolean; owner: string; }
type WsMessage =
  | { type: 'ADD';    payload: Todo }
  | { type: 'UPDATE'; payload: Todo }
  | { type: 'DELETE'; payload: { id: string } };

@Injectable({ providedIn: 'root' })
export class TodoService implements OnDestroy {
  readonly #destroy = inject(DestroyRef);
  readonly #ws      = inject(WsService);
  readonly #todos   = signal<Todo[]>([]);

  readonly todos    = this.#todos.asReadonly();
  readonly done     = computed(() => this.#todos().filter(t => t.done));
  readonly active   = computed(() => this.#todos().filter(t => !t.done));

  constructor() {
    this.#ws.connect();
    this.#ws.messages$
      .pipe(takeUntilDestroyed(this.#destroy))
      .subscribe(msg => this.#apply(msg));
  }

  #apply(msg: WsMessage): void {
    this.#todos.update(todos => {
      switch (msg.type) {
        case 'ADD':    return [...todos, msg.payload];
        case 'UPDATE': return todos.map(t => t.id === msg.payload.id ? msg.payload : t);
        case 'DELETE': return todos.filter(t => t.id !== msg.payload.id);
      }
    });
  }

  add(text: string): void {
    const todo: Todo = { id: crypto.randomUUID(), text, done: false, owner: 'me' };
    this.#ws.send({ type: 'ADD', payload: todo });
    this.#todos.update(ts => [...ts, todo]); // optimistic
  }

  toggle(id: string): void {
    this.#todos.update(ts =>
      ts.map(t => t.id === id ? { ...t, done: !t.done } : t)
    );
    this.#ws.send({ type: 'UPDATE', payload: this.#todos().find(t => t.id === id)! });
  }

  ngOnDestroy(): void { this.#ws.disconnect(); }
}
```

### Todo List Component (OnPush + Virtual Scroll)
```typescript
@Component({
  selector: 'app-todo-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <cdk-virtual-scroll-viewport itemSize="56" style="height:600px">
      <app-todo-item
        *cdkVirtualFor="let todo of todos(); trackBy: trackById"
        [todo]="todo"
        (toggle)="svc.toggle($event)"
      />
    </cdk-virtual-scroll-viewport>
  `
})
export class TodoListComponent {
  readonly svc   = inject(TodoService);
  readonly todos = this.svc.todos;
  trackById = (_: number, t: Todo) => t.id;
}
```

### Patterns Used
- WebSocket + RxJS Subject bridge
- Reconnect strategy with `timer`
- Optimistic updates (apply locally before server confirms)
- Signal-based state (`signal`, `computed`)
- `OnPush` + CDK Virtual Scroll for performance
- `takeUntilDestroyed` + `DestroyRef` for cleanup

---

## Project 3: Custom Component Library

### Specification
Build 5 fully accessible, production-quality Angular components: `Button`, `Modal`, `Dropdown`, `DataTable`, `Toast`. Each must support keyboard navigation, ARIA, theming, and lazy rendering.

### Architecture
```
lib/
├── button/
│   ├── button.component.ts
│   └── button.directive.ts   ← host binding variant
├── modal/
│   ├── modal.component.ts
│   ├── modal.service.ts      ← open programmatically
│   └── focus-trap.directive.ts
├── dropdown/
│   ├── dropdown.component.ts
│   └── dropdown-item.component.ts
├── data-table/
│   ├── data-table.component.ts
│   ├── sort.directive.ts
│   └── pagination.component.ts
├── toast/
│   ├── toast.component.ts
│   └── toast.service.ts      ← global queue
└── index.ts                  ← public API barrel
```

### Modal — Programmatic API with Focus Trap
```typescript
// modal.service.ts
@Injectable({ providedIn: 'root' })
export class ModalService {
  readonly #appRef    = inject(ApplicationRef);
  readonly #injector  = inject(Injector);

  open<T>(component: Type<T>, data?: unknown): ComponentRef<T> {
    const ref = createComponent(component, {
      environmentInjector: this.#appRef.injector,
      elementInjector: Injector.create({
        providers: [{ provide: MODAL_DATA, useValue: data }],
        parent: this.#injector
      })
    });
    this.#appRef.attachView(ref.hostView);
    document.body.appendChild(ref.location.nativeElement);
    return ref;
  }

  close(ref: ComponentRef<unknown>): void {
    this.#appRef.detachView(ref.hostView);
    ref.destroy();
  }
}
```

```typescript
// focus-trap.directive.ts
@Directive({ selector: '[focusTrap]', standalone: true })
export class FocusTrapDirective implements AfterViewInit, OnDestroy {
  readonly #el = inject(ElementRef<HTMLElement>);
  #handler!: (e: KeyboardEvent) => void;

  ngAfterViewInit(): void {
    const focusable = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

    this.#handler = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;
      const nodes = Array.from(this.#el.nativeElement.querySelectorAll<HTMLElement>(focusable));
      const first = nodes[0];
      const last  = nodes[nodes.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault(); last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault(); first.focus();
      }
    };

    document.addEventListener('keydown', this.#handler);
    // auto-focus first element
    const first = this.#el.nativeElement.querySelector<HTMLElement>('[autofocus], button, input');
    first?.focus();
  }

  ngOnDestroy(): void { document.removeEventListener('keydown', this.#handler); }
}
```

### Toast Service (Observer Pattern + Queue)
```typescript
interface Toast { id: string; message: string; type: 'success'|'error'|'info'; duration: number; }

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly #toasts = signal<Toast[]>([]);
  readonly toasts  = this.#toasts.asReadonly();

  show(message: string, type: Toast['type'] = 'info', duration = 4000): void {
    const toast: Toast = { id: crypto.randomUUID(), message, type, duration };
    this.#toasts.update(ts => [...ts, toast]);
    setTimeout(() => this.#dismiss(toast.id), duration);
  }

  #dismiss(id: string): void {
    this.#toasts.update(ts => ts.filter(t => t.id !== id));
  }
}
```

### Data Table (Generic + Sort + Pagination)
```typescript
@Component({
  selector: 'app-data-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <table role="grid">
      <thead>
        <tr>
          @for (col of columns(); track col.key) {
            <th (click)="sort(col.key)" [attr.aria-sort]="ariaSort(col.key)">
              {{ col.label }}
            </th>
          }
        </tr>
      </thead>
      <tbody>
        @for (row of pagedRows(); track row[idKey()]) {
          <tr>
            @for (col of columns(); track col.key) {
              <td>{{ row[col.key] }}</td>
            }
          </tr>
        }
      </tbody>
    </table>
    <app-pagination [total]="sortedRows().length" [(page)]="page" [pageSize]="pageSize()" />
  `
})
export class DataTableComponent<T extends Record<string, any>> {
  readonly columns  = input.required<{ key: string; label: string }[]>();
  readonly rows     = input.required<T[]>();
  readonly idKey    = input<string>('id');
  readonly pageSize = input<number>(20);

  page    = signal(1);
  #sortKey   = signal<string>('');
  #sortDir   = signal<'asc'|'desc'>('asc');

  sortedRows = computed(() => {
    const key = this.#sortKey();
    if (!key) return this.rows();
    return [...this.rows()].sort((a, b) => {
      const dir = this.#sortDir() === 'asc' ? 1 : -1;
      return a[key] < b[key] ? -dir : a[key] > b[key] ? dir : 0;
    });
  });

  pagedRows = computed(() => {
    const size  = this.pageSize();
    const start = (this.page() - 1) * size;
    return this.sortedRows().slice(start, start + size);
  });

  sort(key: string): void {
    if (this.#sortKey() === key)
      this.#sortDir.update(d => d === 'asc' ? 'desc' : 'asc');
    else {
      this.#sortKey.set(key);
      this.#sortDir.set('asc');
    }
  }

  ariaSort(key: string): string {
    if (this.#sortKey() !== key) return 'none';
    return this.#sortDir() === 'asc' ? 'ascending' : 'descending';
  }
}
```

### Patterns Used
- `createComponent` for programmatic rendering
- Focus trap & keyboard navigation (accessibility)
- Observer pattern (Toast queue)
- Generic components with `input()` signals
- `computed` for derived sort/pagination
- ARIA attributes for screen readers

---

## Project 4: Authentication System with JWT & Guards

### Specification
A complete authentication flow: login, register, JWT refresh, route guards, HTTP interceptors, and role-based access control.

### Architecture
```
auth/
├── auth.service.ts         ← login/logout/refresh logic
├── token.service.ts        ← JWT storage + decode (no library)
├── auth.interceptor.ts     ← attach token + 401 refresh
├── auth.guard.ts           ← route protection
├── role.guard.ts           ← role-based access
└── auth.store.ts           ← signal-based auth state
```

### Token Service (JWT Without a Library)
```typescript
@Injectable({ providedIn: 'root' })
export class TokenService {
  readonly #ACCESS_KEY  = 'access_token';
  readonly #REFRESH_KEY = 'refresh_token';

  setTokens(access: string, refresh: string): void {
    sessionStorage.setItem(this.#ACCESS_KEY, access);
    localStorage.setItem(this.#REFRESH_KEY, refresh);
  }

  getAccess(): string | null  { return sessionStorage.getItem(this.#ACCESS_KEY); }
  getRefresh(): string | null { return localStorage.getItem(this.#REFRESH_KEY); }

  clearTokens(): void {
    sessionStorage.removeItem(this.#ACCESS_KEY);
    localStorage.removeItem(this.#REFRESH_KEY);
  }

  decodePayload<T>(token: string): T {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64)) as T;
  }

  isExpired(token: string): boolean {
    const { exp } = this.decodePayload<{ exp: number }>(token);
    return Date.now() >= exp * 1000;
  }
}
```

### Auth Store (Signal-Based)
```typescript
interface AuthState {
  user:  { id: string; email: string; roles: string[] } | null;
  ready: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthStore {
  readonly #state  = signal<AuthState>({ user: null, ready: false });
  readonly user    = computed(() => this.#state().user);
  readonly isAuth  = computed(() => this.#state().user !== null);
  readonly roles   = computed(() => this.#state().user?.roles ?? []);
  readonly isReady = computed(() => this.#state().ready);

  hasRole(role: string): boolean { return this.roles().includes(role); }

  setUser(u: AuthState['user']): void {
    this.#state.update(s => ({ ...s, user: u, ready: true }));
  }

  clear(): void { this.#state.set({ user: null, ready: true }); }
}
```

### HTTP Interceptor — Attach Token + Refresh on 401
```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token  = inject(TokenService);
  const auth   = inject(AuthService);
  const isRefreshing = inject(IS_REFRESHING);

  const accessToken = token.getAccess();
  if (accessToken) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } });
  }

  return next(req).pipe(
    catchError(err => {
      if (err.status === 401 && !req.url.includes('/refresh')) {
        return auth.refresh$().pipe(
          switchMap(newToken => {
            return next(req.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` }
            }));
          })
        );
      }
      return throwError(() => err);
    })
  );
};
```

### Route Guards (Functional)
```typescript
// auth.guard.ts
export const authGuard: CanActivateFn = () => {
  const authStore = inject(AuthStore);
  const router    = inject(Router);
  if (authStore.isAuth()) return true;
  return router.createUrlTree(['/login']);
};

// role.guard.ts
export function roleGuard(requiredRole: string): CanActivateFn {
  return () => {
    const authStore = inject(AuthStore);
    const router    = inject(Router);
    if (authStore.hasRole(requiredRole)) return true;
    return router.createUrlTree(['/forbidden']);
  };
}

// routes.ts
export const routes: Routes = [
  { path: 'admin', loadComponent: () => import('./admin/admin.component'),
    canActivate: [authGuard, roleGuard('admin')] },
];
```

### Patterns Used
- JWT decode without external library (reduces bundle)
- `sessionStorage` for access token (XSS safer than `localStorage`)
- `localStorage` for refresh token (persists across tabs)
- Interceptor for automatic token refresh
- Functional guards (Angular 15+)
- Signal-based auth state
- Role-based access control

---

## Project 5: Full Angular Dashboard Application

### Specification
Combine all previous projects into a single cohesive application: authenticated users can manage a real-time todo list, view analytics, and administer users — with role-based access, live updates, virtual scroll, custom components, and a Signal-based state architecture.

### Architecture Overview
```
app/
├── core/
│   ├── auth/              ← Project 4
│   ├── ws/                ← Project 2
│   └── store/             ← Project 1
├── lib/                   ← Project 3 (component library)
├── features/
│   ├── dashboard/
│   │   ├── dashboard.component.ts    ← overview widgets
│   │   └── analytics.service.ts     ← aggregated stats
│   ├── todos/             ← Project 2 (full feature)
│   ├── admin/             ← role-guarded
│   │   ├── users-table/   ← DataTable + pagination
│   │   └── audit-log/     ← virtual scroll + WS feed
│   └── settings/
│       └── profile-form/  ← Reactive Forms + async validators
├── app.routes.ts          ← lazy-loaded feature routes
└── app.config.ts          ← provideRouter, provideHttpClient, etc.
```

### App Config (Modern Standalone Bootstrap)
```typescript
// app.config.ts
import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withPreloading, PreloadAllModules } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),     // Signals-native, no Zone.js
    provideRouter(routes, withPreloading(PreloadAllModules)),
    provideHttpClient(withInterceptors([authInterceptor])),
  ]
};
```

### Dashboard Analytics (Derived Signals)
```typescript
@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  readonly #todos = inject(TodoService);

  readonly stats = computed(() => {
    const all    = this.#todos.todos();
    const done   = all.filter(t => t.done).length;
    const active = all.length - done;
    const pct    = all.length ? Math.round((done / all.length) * 100) : 0;
    return { total: all.length, done, active, completionPct: pct };
  });

  readonly recentActivity = computed(() =>
    [...this.#todos.todos()]
      .sort((a, b) => b.updatedAt - a.updatedAt)
      .slice(0, 5)
  );
}
```

### Dashboard Component
```typescript
@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AsyncPipe, CurrencyPipe, DataTableComponent, ToastComponent],
  template: `
    <section class="stats-grid">
      <app-stat-card label="Total Tasks"    [value]="stats().total"         />
      <app-stat-card label="Completed"      [value]="stats().done"          />
      <app-stat-card label="Active"         [value]="stats().active"        />
      <app-stat-card label="Completion"     [value]="stats().completionPct" unit="%" />
    </section>

    <section>
      <h2>Recent Activity</h2>
      <app-data-table
        [columns]="columns"
        [rows]="recentActivity()"
        [pageSize]="5"
      />
    </section>
  `
})
export class DashboardComponent {
  readonly analytics    = inject(AnalyticsService);
  readonly stats        = this.analytics.stats;
  readonly recentActivity = this.analytics.recentActivity;
  readonly columns      = [
    { key: 'text', label: 'Task' },
    { key: 'owner', label: 'Owner' },
    { key: 'done', label: 'Status' },
  ];
}
```

### Lazy-Loaded Routes
```typescript
// app.routes.ts
export const routes: Routes = [
  { path: '',        redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'login',   loadComponent: () => import('./features/auth/login.component') },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component'),
  },
  {
    path: 'todos',
    canActivate: [authGuard],
    loadChildren: () => import('./features/todos/todos.routes'),
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard('admin')],
    loadChildren: () => import('./features/admin/admin.routes'),
  },
  { path: '**', loadComponent: () => import('./features/not-found/not-found.component') },
];
```

---

## 15.1 Technology Mastery Checklist

After completing all five projects, you should be able to confidently demonstrate:

### JavaScript Deep Mastery
- [ ] Prototype chain manipulation and `Object.create` patterns
- [ ] All five `this` binding rules + edge cases
- [ ] Closure-based module and memoization patterns
- [ ] Full event loop mechanics including microtask/macrotask ordering
- [ ] Implement `Promise.all`, `debounce`, `curry`, `LRU cache` from scratch
- [ ] Functional composition: `pipe`, `compose`, functor/monad patterns
- [ ] ES2020–ES2024 feature mastery (optional chaining, nullish, `using`, etc.)
- [ ] Memory management: WeakRef, FinalizationRegistry, identifying leaks

### Angular Mastery
- [ ] Build a production-grade Signal-based state management solution
- [ ] Implement `OnPush` + `trackBy` + immutable state correctly on all lists
- [ ] Write and distinguish `switchMap`, `mergeMap`, `concatMap`, `exhaustMap` with real examples
- [ ] Build an HTTP interceptor with JWT refresh and retry logic
- [ ] Implement functional route guards with role-based access
- [ ] Build accessible components with focus management and ARIA
- [ ] Configure zonelessly with `provideZonelessChangeDetection`
- [ ] Set up lazy-loaded routes with preloading strategy
- [ ] Write unit tests with `TestBed`, `HttpClientTestingModule`, marble testing
- [ ] Debug change detection with Angular DevTools

### Architecture Mastery
- [ ] Apply at least 5 GoF design patterns in Angular context
- [ ] Design a reactive state architecture with single source of truth
- [ ] Structure a large app with feature modules, shared library, and core
- [ ] Implement optimistic updates with rollback
- [ ] Design for accessibility (WCAG 2.1 AA) from the start

---

## 15.2 Learning Path Forward

| Area                      | Recommended Next                                    |
|---------------------------|-----------------------------------------------------|
| Testing                   | Angular Testing Masterclass, Cypress component tests|
| Server-Side Rendering      | Angular Universal / SSR with `@angular/ssr`         |
| Micro-frontends           | Module Federation with Angular                      |
| Advanced RxJS             | Learn Marble Testing, custom operators              |
| Performance               | Web Vitals, Angular DevTools profiling              |
| Signals deep dive         | Angular RFC docs, `linkedSignal`, `resource()`      |
| TypeScript                | Conditional types, template literal types, `infer`  |
| Node.js backend           | NestJS (same DI patterns as Angular)                |

---

## 15.3 Final Mental Model — JavaScript to Angular

```
JavaScript Fundamentals
        │
        ▼
   Closures + Scope  ──────────────────────► DI Tokens (InjectionToken)
        │
        ▼
   Prototype + Classes  ─────────────────► @Component, @Injectable, @Directive
        │
        ▼
   Async + Event Loop  ──────────────────► RxJS Observables + async pipe
        │
        ▼
   Functional Programming  ─────────────► Pure pipes, OnPush, immutable state
        │
        ▼
   Proxy + Reactivity  ──────────────────► Signals (signal, computed, effect)
        │
        ▼
   Modules + Tree Shaking  ─────────────► Standalone components, lazy loading
        │
        ▼
   Design Patterns  ────────────────────► Angular architecture (services, stores,
                                          interceptors, guards, resolvers)
```

---

## 15.4 Congratulations — You Have Reached Mastery Level

You have completed all 15 modules covering:

| Module | Topic |
|--------|-------|
| 1  | JavaScript Fundamentals |
| 2  | Functions & Execution Context |
| 3  | Arrays Mastery |
| 4  | Objects Mastery |
| 5  | Scope, Hoisting & Closures |
| 6  | Async JavaScript |
| 7  | Prototype & OOP |
| 8  | DOM & Browser APIs |
| 9  | Modules & Modern ES6+ |
| 10 | Functional Programming |
| 11 | Performance Optimization |
| 12 | Advanced Engineering Patterns |
| 13 | Real-World Angular JavaScript |
| 14 | Interview Preparation |
| 15 | Mastery Projects |

> **This document is your complete JavaScript mastery reference for Angular development.** Bookmark it, return to it before interviews, and use the projects as portfolio pieces.

---

---

# JavaScript Interview Questions Bank (120 Questions — Basic to Advanced)

> 120 questions organised from basic to advanced. Questions 1–120 cover all core JS topics. Each answer is concise but complete enough for a real interview.

---

## Questions 1–15 — JavaScript Basics

**Q1. What is JavaScript?**
> JavaScript is a lightweight, interpreted, single-threaded, dynamically typed programming language. It runs in browsers (via JS engines like V8) and on servers (Node.js). It is the only language natively understood by browsers and is the backbone of interactive web applications.

**Q2. What are the primitive data types in JavaScript?**
> There are 7 primitives: `string`, `number`, `bigint`, `boolean`, `undefined`, `null`, `symbol`. Primitives are immutable and stored by value. Everything else (arrays, objects, functions) is stored by reference.

**Q3. What is the difference between `undefined` and `not defined`?**
> `undefined` means a variable has been declared but not assigned a value — it exists in memory. `not defined` means a variable was never declared — accessing it throws a `ReferenceError`.
```js
let x;
console.log(x);   // undefined (declared, no value)
console.log(y);   // ReferenceError: y is not defined
```

**Q4. What is dynamic typing?**
> In JavaScript, variables do not have fixed types — the type is determined by the value assigned at runtime. The same variable can hold a `string`, then a `number`, then an `object`.
```js
let v = 'hello'; // string
v = 42;          // now number
v = true;        // now boolean
```

**Q5. What are JavaScript data types grouped into?**
> **Primitive** (stored by value): `string`, `number`, `bigint`, `boolean`, `null`, `undefined`, `symbol`.
> **Non-primitive / Reference** (stored by reference): `Object`, `Array`, `Function`, `Map`, `Set`, `Date`, etc.

**Q6. Explain type coercion with examples.**
> Implicit conversion of one type to another by JavaScript during operations.
```js
'5' + 3       // '53'  — number coerced to string (+ prefers string)
'5' - 3       // 2     — string coerced to number (- has no string meaning)
true + 1      // 2     — true → 1
false + '1'   // 'false1'
null + 1      // 1     — null → 0
undefined + 1 // NaN   — undefined → NaN
```

**Q7. What is `NaN` and how do you check for it?**
> `NaN` (Not-a-Number) is the result of an invalid numeric operation (e.g., `'abc' * 2`). It is the only value in JavaScript not equal to itself (`NaN !== NaN`). Use `Number.isNaN(value)` to check — NOT `isNaN()` which coerces first.
```js
Number.isNaN(NaN);      // true
Number.isNaN('NaN');    // false (no coercion)
isNaN('NaN');           // true  (coerces string → NaN first — misleading)
```

**Q8. What is the difference between `==` and `===`?**
> `==` (loose equality) compares values after type coercion. `===` (strict equality) compares value AND type without coercion. Always prefer `===`.
```js
0 == false     // true  (false → 0)
0 === false    // false (different types)
null == undefined  // true
null === undefined // false
```

**Q9. What are truthy and falsy values?**
> **Falsy** (8 total): `false`, `0`, `-0`, `0n`, `''`, `null`, `undefined`, `NaN`.
> Everything else is **truthy** — including `[]`, `{}`, `'0'`, `'false'`, `-1`, `Infinity`.
```js
if ([])  console.log('truthy'); // logs — empty array is truthy
if ('')  console.log('truthy'); // does NOT log — empty string is falsy
```

**Q10. What is the `typeof` operator?**
> Returns a string indicating the type of a value.
```js
typeof 42           // 'number'
typeof 'hi'         // 'string'
typeof true         // 'boolean'
typeof undefined    // 'undefined'
typeof null         // 'object'  ← historic bug
typeof {}           // 'object'
typeof []           // 'object'  ← arrays are objects
typeof function(){} // 'function'
typeof Symbol()     // 'symbol'
typeof 42n          // 'bigint'
```

**Q11. What is the difference between `null` and `undefined`?**
> `undefined` — JS's default for uninitialized variables, missing arguments, and missing object properties. The engine sets this automatically.
> `null` — intentional absence of value, set explicitly by the developer. Signals "this variable deliberately has no object."
```js
let a;         // undefined (auto)
let b = null;  // null (intentional)
typeof null;   // 'object' (historic bug — null is NOT an object)
```

**Q12. How do you comment in JavaScript?**
> Single-line: `// comment`
> Multi-line: `/* comment */`
> JSDoc: `/** @param {string} name */` — generates documentation and enables IDE intellisense.

**Q13. What are template literals?**
> Template literals (backticks `` ` ``) allow embedded expressions and multi-line strings without concatenation.
```js
const name  = 'Alice';
const multi = `Hello, ${name}!
This spans
multiple lines.`;

// Tagged template
function tag(strings, ...values) {
  return strings.raw[0] + values[0].toUpperCase();
}
tag`Hello ${name}` // 'Hello ALICE'
```

**Q14. What is the difference between `let`, `const`, and `var`?**
> | Feature      | var         | let         | const       |
> |--------------|-------------|-------------|-------------|
> | Scope        | Function    | Block       | Block       |
> | Hoisting     | Yes (undefined) | Yes (TDZ) | Yes (TDZ) |
> | Re-declare   | Yes         | No          | No          |
> | Re-assign    | Yes         | Yes         | No          |
> | Global prop  | Yes (window)| No          | No          |

**Q15. What is string immutability in JavaScript?**
> Primitive strings cannot be mutated — string methods always return a new string, never modifying the original. The variable can be reassigned to a new string, but the original string value in memory is unchanged.
```js
let s = 'hello';
s.toUpperCase();  // returns 'HELLO' — original unchanged
console.log(s);   // still 'hello'
s = s.toUpperCase(); // re-assign to new string
console.log(s);   // 'HELLO'
```

---

> **Questions 1–15 complete.** Type `continue` for Questions 16–30.

---

## Questions 16–30 — Functions & Scope

**Q16. What are the different ways to define a function in JavaScript?**
> 1. **Function declaration** — hoisted fully, callable before definition.
> 2. **Function expression** — assigned to a variable, not hoisted.
> 3. **Arrow function** — no own `this`, `arguments`, or `prototype`.
> 4. **IIFE** — immediately invoked, creates private scope.
> 5. **Method shorthand** — inside object literals.
> 6. **Generator function** — `function*`, yields values lazily.
> 7. **Async function** — always returns a Promise.
```js
function   declared()  {}          // declaration
const expr = function() {};        // expression
const arrow = () => {};            // arrow
(function() {})();                 // IIFE
const obj = { method() {} };       // shorthand
function* gen() { yield 1; }       // generator
async function load() {}           // async
```

**Q17. What is a first-class function?**
> Functions in JavaScript are first-class citizens — they can be assigned to variables, passed as arguments to other functions, returned from functions, and stored in data structures. This enables higher-order functions and functional programming patterns.
```js
const greet = name => `Hello, ${name}`;

function execute(fn, value) { return fn(value); } // passed as arg
const result = execute(greet, 'Alice'); // 'Hello, Alice'

function multiplier(x) {
  return n => n * x;                // returned from function
}
const double = multiplier(2);
double(5); // 10
```

**Q18. What is a higher-order function?**
> A function that either accepts a function as an argument or returns a function (or both). `map`, `filter`, `reduce`, `forEach`, `setTimeout` are all higher-order functions.
```js
const numbers = [1, 2, 3, 4, 5];

// accepts a function
const evens = numbers.filter(n => n % 2 === 0); // [2, 4]

// returns a function
const add = x => y => x + y;
const add5 = add(5);
add5(3); // 8
```

**Q19. What is scope in JavaScript?**
> Scope determines the visibility and accessibility of variables. JavaScript has four scope types:
> 1. **Global** — accessible everywhere.
> 2. **Function** — variables declared `var` inside a function.
> 3. **Block** — `let`/`const` inside `{}`.
> 4. **Module** — variables in ES modules are module-scoped by default.
```js
let global = 'global';
function fn() {
  let local = 'local';
  if (true) {
    let block = 'block'; // only inside this {}
    console.log(local);  // accessible — outer function scope
  }
  console.log(block); // ReferenceError
}
```

**Q20. What is the scope chain?**
> When a variable is accessed, JS looks in the current scope first, then walks outward through parent scopes until it finds the variable or reaches the global scope. If not found, a `ReferenceError` is thrown. This chain is fixed at **definition time** (lexical scope), not at call time.
```js
const x = 'global';
function outer() {
  const x = 'outer';
  function inner() {
    console.log(x); // 'outer' — found in outer scope, not global
  }
  inner();
}
```

**Q21. What is hoisting?**
> Before code executes, the JS engine scans the scope and allocates memory for declarations:
> - **Function declarations**: fully hoisted — callable before the line they appear on.
> - **`var`**: hoisted and initialised to `undefined`.
> - **`let`/`const`/`class`**: hoisted but in TDZ — accessing before declaration throws `ReferenceError`.
```js
sayHi();             // works — function declaration hoisted
function sayHi() { console.log('hi'); }

console.log(n);      // undefined — var hoisted
var n = 5;

console.log(m);      // ReferenceError — TDZ
let m = 10;
```

**Q22. What is the Temporal Dead Zone (TDZ)?**
> The period from the start of a block scope until the `let`/`const` declaration is reached. The variable exists in the scope (hoisted) but is in an uninitialised state. Any access in this zone throws a `ReferenceError`. This prevents using variables before they are intentionally defined.
```js
{
  // TDZ for `value` starts here
  console.log(value); // ReferenceError
  let value = 42;     // TDZ ends here
  console.log(value); // 42
}
```

**Q23. What is a closure?**
> A closure is a function that retains a live reference to variables in its outer lexical scope even after that outer function has returned. The inner function "closes over" those variables.
```js
function bankAccount(initialBalance) {
  let balance = initialBalance; // closed over

  return {
    deposit:  amt => balance += amt,
    withdraw: amt => balance -= amt,
    getBalance: () => balance,
  };
}
const acc = bankAccount(100);
acc.deposit(50);
acc.getBalance(); // 150 — balance persists
```

**Q24. What is the difference between lexical scope and dynamic scope?**
> **Lexical scope** (used by JavaScript): a function's scope is determined by where it is **defined** in the source code. The scope chain is fixed at author time.
> **Dynamic scope** (not used in JS, used by Bash/older Lisps): scope is determined by where the function is **called**. `this` in JavaScript behaves dynamically (call-site matters), which is why arrow functions were introduced to restore lexical `this`.

**Q25. Explain the `arguments` object.**
> Available inside regular (non-arrow) functions, `arguments` is an array-like object containing all arguments passed to the function — even if not declared as parameters. It lacks array methods like `map`. Use **rest parameters** (`...args`) in modern code instead.
```js
function sum() {
  let total = 0;
  for (let i = 0; i < arguments.length; i++) total += arguments[i];
  return total;
}
sum(1, 2, 3); // 6

// Modern equivalent
const sumModern = (...args) => args.reduce((a, b) => a + b, 0);
```

**Q26. What are default parameters?**
> ES6 allows function parameters to have default values when the caller passes `undefined` or omits the argument.
```js
function greet(name = 'World', greeting = 'Hello') {
  return `${greeting}, ${name}!`;
}
greet();                    // 'Hello, World!'
greet('Alice');             // 'Hello, Alice!'
greet('Bob', 'Hi');         // 'Hi, Bob!'
greet(undefined, 'Hey');    // 'Hey, World!' — undefined triggers default
greet(null, 'Hey');         // 'Hey, null!'  — null does NOT trigger default
```

**Q27. What is the rest parameter?**
> The `...rest` syntax collects all remaining arguments into a real array. Unlike `arguments`, it is a true array and can use all array methods. Must be the last parameter.
```js
function log(level, ...messages) {
  console.log(`[${level}]`, messages.join(' '));
}
log('INFO', 'Server', 'started', 'on', '3000');
// [INFO] Server started on 3000
```

**Q28. What is the spread operator?**
> `...` spreads an iterable (array, string, Set) into individual elements. Used in function calls, array literals, and object literals.
```js
const a = [1, 2, 3];
const b = [4, 5, 6];
const combined = [...a, ...b];         // [1,2,3,4,5,6]
Math.max(...a);                        // 3

const obj1 = { x: 1 };
const obj2 = { y: 2 };
const merged = { ...obj1, ...obj2 };   // { x:1, y:2 }
```

**Q29. What is an IIFE and why is it used?**
> An **Immediately Invoked Function Expression** — a function that runs as soon as it is defined.
> Uses: create a private scope (avoid polluting global), initialise once, create module-like isolation (pre-ES modules pattern).
```js
const result = (function() {
  const private = 'secret';
  return { getSecret: () => private };
})();

result.getSecret(); // 'secret'
// private is inaccessible from outside
```

**Q30. What is function currying?**
> Transforming a function that takes multiple arguments into a sequence of functions, each taking one argument. Enables partial application and point-free composition.
```js
// Manual curry
const multiply = a => b => a * b;
const triple = multiply(3);
triple(5); // 15

// Generic curry helper
function curry(fn) {
  return function curried(...args) {
    return args.length >= fn.length
      ? fn(...args)
      : (...more) => curried(...args, ...more);
  };
}

const add = curry((a, b, c) => a + b + c);
add(1)(2)(3); // 6
add(1, 2)(3); // 6
add(1)(2, 3); // 6
```

---

> **Questions 16–30 complete.** Type `continue` for Questions 31–45.

---

## Questions 31–45 — Arrays, Objects & Destructuring

**Q31. What is the difference between `slice` and `splice`?**
> `slice(start, end)` — non-mutating, returns a new array from `start` up to (not including) `end`.
> `splice(start, deleteCount, ...items)` — mutating, removes/replaces/inserts elements in-place, returns removed elements.
```js
const arr = [1, 2, 3, 4, 5];

arr.slice(1, 3);          // [2, 3] — original unchanged
arr.splice(1, 2);         // returns [2, 3] — arr is now [1, 4, 5]
arr.splice(1, 0, 9, 10);  // inserts 9, 10 at index 1, nothing removed
```

**Q32. What is the difference between `map`, `filter`, `reduce`, and `forEach`?**
> | Method    | Mutates? | Returns          | Use for                         |
> |-----------|----------|------------------|---------------------------------|
> | `map`     | No       | New array (same length) | Transform each element   |
> | `filter`  | No       | New array (shorter)     | Select matching elements |
> | `reduce`  | No       | Single value            | Accumulate to one result |
> | `forEach` | No       | `undefined`             | Side effects only        |
```js
const nums = [1, 2, 3, 4, 5];
nums.map(n => n * 2);            // [2,4,6,8,10]
nums.filter(n => n % 2 === 0);   // [2,4]
nums.reduce((sum, n) => sum + n, 0); // 15
nums.forEach(n => console.log(n));   // undefined
```

**Q33. How do you remove duplicates from an array?**
> Best approach: use `Set` — it only stores unique values.
```js
const arr = [1, 2, 2, 3, 3, 4];
const unique = [...new Set(arr)];         // [1, 2, 3, 4]

// Alternative for objects (by key)
const people = [{id:1,name:'Alice'},{id:2,name:'Bob'},{id:1,name:'Alice'}];
const byId = [...new Map(people.map(p => [p.id, p])).values()];
```

**Q34. How do you flatten a nested array?**
```js
const nested = [1, [2, [3, [4]]]];

nested.flat();          // [1, 2, [3, [4]]]  — one level
nested.flat(Infinity);  // [1, 2, 3, 4]       — all levels
nested.flat(2);         // [1, 2, 3, [4]]     — two levels

// Manual recursive
const flatten = arr => arr.reduce((acc, v) =>
  Array.isArray(v) ? acc.concat(flatten(v)) : [...acc, v], []);
```

**Q35. What is array destructuring?**
> A syntax to unpack array elements into variables by position.
```js
const [a, b, c] = [10, 20, 30];      // a=10, b=20, c=30
const [x, , z]  = [1, 2, 3];          // skip element — x=1, z=3
const [head, ...tail] = [1, 2, 3, 4]; // head=1, tail=[2,3,4]

// Default values
const [p = 0, q = 0] = [5];           // p=5, q=0

// Swap variables
let m = 1, n = 2;
[m, n] = [n, m];                      // m=2, n=1
```

**Q36. What is object destructuring?**
> Unpack object properties into variables by name.
```js
const user = { name: 'Alice', age: 30, role: 'admin' };

const { name, age }        = user;            // name='Alice', age=30
const { name: userName }   = user;            // rename: userName='Alice'
const { role = 'user' }    = {};              // default: role='user'
const { name: n, ...rest } = user;            // rest = { age:30, role:'admin' }

// Nested
const { address: { city } } = { address: { city: 'NY' } };
```

**Q37. What is the difference between `Object.keys`, `Object.values`, and `Object.entries`?**
```js
const obj = { a: 1, b: 2, c: 3 };

Object.keys(obj);    // ['a', 'b', 'c']
Object.values(obj);  // [1, 2, 3]
Object.entries(obj); // [['a',1], ['b',2], ['c',3]]

// Convert entries back to object
const doubled = Object.fromEntries(
  Object.entries(obj).map(([k, v]) => [k, v * 2])
);
// { a:2, b:4, c:6 }
```

**Q38. What is the difference between shallow copy and deep copy?**
> **Shallow copy**: copies the top-level properties only. Nested objects share the same reference.
> **Deep copy**: recursively copies the entire structure — no shared references.
```js
const orig = { a: 1, nested: { b: 2 } };

// Shallow
const shallow = { ...orig };
shallow.nested.b = 99;
console.log(orig.nested.b); // 99 — shared reference modified

// Deep
const deep = structuredClone(orig);
deep.nested.b = 99;
console.log(orig.nested.b); // 2 — independent copy
```

**Q39. What is `Object.freeze` and `Object.seal`?**
> `Object.freeze(obj)` — prevents adding, removing, or modifying any properties. Shallow — nested objects are not frozen.
> `Object.seal(obj)` — prevents adding/removing properties but allows modifying existing ones.
```js
const frozen = Object.freeze({ x: 1 });
frozen.x = 99;      // silently fails (throws in strict mode)
frozen.y = 2;       // silently fails
console.log(frozen.x); // 1

const sealed = Object.seal({ x: 1 });
sealed.x = 99;      // allowed — modifying existing
sealed.y = 2;       // silently fails — can't add
```

**Q40. What is the difference between `Map` and a plain object `{}`?**
> | Feature        | Object `{}`                | `Map`                        |
> |----------------|----------------------------|------------------------------|
> | Key types      | string / symbol only       | Any type (objects, functions)|
> | Order          | Insertion order (mostly)   | Guaranteed insertion order   |
> | Size           | `Object.keys(o).length`    | `map.size`                   |
> | Prototype      | Has inherited keys         | Clean — no inherited keys    |
> | Iteration      | Requires `Object.entries`  | Directly iterable            |
> | Performance    | Slower for frequent adds/removes | Optimised for this    |
```js
const map = new Map();
map.set({ id: 1 }, 'object key'); // object as key — impossible with {}
map.set(42, 'number key');
map.size; // 2
```

**Q41. What is `Set` and when do you use it?**
> `Set` is a collection of unique values of any type. Best for deduplication and membership checks.
```js
const set = new Set([1, 2, 2, 3, 3]);
set.size;         // 3
set.has(2);       // true
set.add(4);
set.delete(1);

// Convert to array
[...set];         // [2, 3, 4]

// Fast O(1) lookup vs array O(n)
const haystack = new Set([1, 2, 3, 4, 5]);
haystack.has(3);  // O(1) — much faster than array.includes for large sets
```

**Q42. What are `WeakMap` and `WeakSet`?**
> Versions of `Map`/`Set` where keys (WeakMap) or values (WeakSet) must be objects and are held **weakly** — they don't prevent garbage collection. Not enumerable.
> Use cases: associate metadata with DOM nodes without memory leaks, private class data, caching where keys may be GC'd.
```js
const cache = new WeakMap();

function process(element) {
  if (cache.has(element)) return cache.get(element);
  const result = heavyComputation(element);
  cache.set(element, result);
  return result;
}
// When DOM element is removed, cache entry is GC'd automatically
```

**Q43. How do you check if a property exists in an object?**
> 1. `'key' in obj` — checks own AND inherited properties.
> 2. `obj.hasOwnProperty('key')` — checks own properties only.
> 3. `Object.hasOwn(obj, 'key')` — modern, safer alternative to `hasOwnProperty`.
> 4. `obj.key !== undefined` — unreliable (property could be explicitly set to `undefined`).
```js
const obj = { a: 1 };
'a' in obj;                  // true
'toString' in obj;           // true  — inherited from Object.prototype
obj.hasOwnProperty('a');     // true
obj.hasOwnProperty('toString'); // false
Object.hasOwn(obj, 'a');     // true  — preferred modern approach
```

**Q44. What is optional chaining (`?.`)?**
> Safely access deeply nested properties without throwing `TypeError` if an intermediate value is `null` or `undefined`. Returns `undefined` instead.
```js
const user = { profile: { address: { city: 'London' } } };

user?.profile?.address?.city;    // 'London'
user?.settings?.theme;           // undefined — no error
user?.getName?.();               // undefined — safe method call
user?.friends?.[0];              // undefined — safe index access

// Before ES2020 (verbose)
user && user.profile && user.profile.address && user.profile.address.city;
```

**Q45. What is the nullish coalescing operator (`??`)?**
> Returns the right-hand side value only when the left side is `null` or `undefined` — NOT for other falsy values like `0`, `''`, or `false`. This is the key difference from `||`.
```js
const config = { timeout: 0, label: '' };

config.timeout || 3000;   // 3000 — WRONG: 0 is falsy, gets replaced
config.timeout ?? 3000;   // 0    — CORRECT: 0 is not null/undefined

config.label || 'default'; // 'default' — WRONG: '' is falsy
config.label ?? 'default'; // ''         — CORRECT

// Combine with optional chaining
const port = server?.config?.port ?? 8080;
```

---

> **Questions 31–45 complete.** Type `continue` for Questions 46–60.

---

## Questions 46–60 — Prototypes, Classes & OOP

**Q46. What is the prototype chain?**
> Every JavaScript object has an internal `[[Prototype]]` link pointing to another object (its prototype). When you access a property, JS looks in the object first, then walks up the chain through each prototype until it finds the property or reaches `Object.prototype` (whose prototype is `null`).
```js
const animal = { breathes: true };
const dog    = Object.create(animal);
dog.barks    = true;

dog.barks;    // true  — own property
dog.breathes; // true  — found on prototype (animal)
dog.flies;    // undefined — not found anywhere in chain

Object.getPrototypeOf(dog) === animal; // true
```

**Q47. What does the `new` keyword do step by step?**
> 1. Creates a new empty object `{}`.
> 2. Sets its `[[Prototype]]` to `Constructor.prototype`.
> 3. Executes the constructor with `this` bound to the new object.
> 4. Returns the new object — unless the constructor explicitly returns a **different** object.
```js
function Car(make) { this.make = make; }
Car.prototype.drive = function() { return `Driving ${this.make}`; };

const car = new Car('Toyota');
car.drive();                          // 'Driving Toyota'
Object.getPrototypeOf(car) === Car.prototype; // true
car instanceof Car;                   // true
```

**Q48. What is the difference between `prototype` and `__proto__`?**
> `Constructor.prototype` — a property of **functions/classes**. It is the object that all instances created with `new Constructor` will inherit from.
> `instance.__proto__` (or `Object.getPrototypeOf(instance)`) — the actual `[[Prototype]]` link **on an instance**, pointing to its constructor's `prototype`.
```js
function Person(name) { this.name = name; }
const alice = new Person('Alice');

Person.prototype.constructor === Person; // true
alice.__proto__ === Person.prototype;    // true
alice.__proto__.__proto__ === Object.prototype; // true
alice.__proto__.__proto__.__proto__ === null;   // end of chain
```

**Q49. What are ES6 classes? Are they truly new?**
> ES6 `class` is **syntactic sugar** over JavaScript's existing prototype-based inheritance. Under the hood, a class creates a constructor function and assigns methods to its `prototype`. Classes are NOT a new object model — they are a cleaner syntax for the same prototype chain.
```js
class Animal {
  #name; // private field
  constructor(name) { this.#name = name; }
  speak() { return `${this.#name} makes a sound`; }
  get name() { return this.#name; }
}

class Dog extends Animal {
  speak() {
    return `${this.name} barks`; // accesses via getter
  }
}

const d = new Dog('Rex');
d.speak();        // 'Rex barks'
d instanceof Dog;    // true
d instanceof Animal; // true
```

**Q50. What is the difference between `class` fields and constructor assignments?**
> Class fields (ES2022) are declared directly in the class body — public or private (`#`). They are initialised before the constructor body runs. Constructor assignments are explicit `this.x = val` inside `constructor()`.
```js
class Counter {
  // Public class field — initialised per instance
  count = 0;

  // Private field — only accessible inside the class
  #step = 1;

  // Static field — on the class itself, not instances
  static instances = 0;

  constructor() { Counter.instances++; }

  increment() { this.count += this.#step; }
}
```

**Q51. What is inheritance and how is it achieved in ES6?**
> Inheritance lets one class acquire properties and methods from another via `extends`. `super()` must be called in the child constructor before using `this`. `super.method()` calls the parent's version of a method.
```js
class Shape {
  constructor(color) { this.color = color; }
  area() { return 0; }
  toString() { return `${this.constructor.name}: color=${this.color}, area=${this.area()}`; }
}

class Circle extends Shape {
  constructor(color, radius) {
    super(color);           // must call super first
    this.radius = radius;
  }
  area() { return Math.PI * this.radius ** 2; }
}

new Circle('red', 5).toString();
// 'Circle: color=red, area=78.53...'
```

**Q52. What is the difference between `instanceof` and `typeof`?**
> `typeof` — returns a string of the primitive type. Unreliable for objects (`typeof []` is `'object'`).
> `instanceof` — checks if an object was created by a specific constructor by walking the prototype chain.
```js
typeof 42;              // 'number'
typeof 'hi';            // 'string'
typeof null;            // 'object' ← historic bug
typeof [];              // 'object' ← not useful

[] instanceof Array;    // true
[] instanceof Object;   // true — Array inherits from Object
'hi' instanceof String; // false — primitive, not instance
```

**Q53. What are static methods and properties?**
> Static members belong to the **class itself**, not to instances. Called on the class directly. Used for utility functions, factory methods, or shared state.
```js
class MathHelper {
  static PI = 3.14159;

  static circleArea(r) { return MathHelper.PI * r * r; }
  static add(a, b)      { return a + b; }
}

MathHelper.circleArea(5);  // 78.53 — called on class
const m = new MathHelper();
m.circleArea(5);           // TypeError — not on instance
```

**Q54. What are getters and setters?**
> Accessors that allow computed property reads/writes with validation logic. Defined with `get` and `set` keywords.
```js
class Temperature {
  #celsius;
  constructor(c) { this.#celsius = c; }

  get fahrenheit() { return this.#celsius * 9/5 + 32; }
  set fahrenheit(f) { this.#celsius = (f - 32) * 5/9; }

  get celsius() { return this.#celsius; }
  set celsius(c) {
    if (c < -273.15) throw new RangeError('Below absolute zero');
    this.#celsius = c;
  }
}

const t = new Temperature(100);
t.fahrenheit;        // 212
t.fahrenheit = 32;
t.celsius;           // 0
```

**Q55. What is mixins and composition vs inheritance?**
> JavaScript only supports single inheritance via `extends`. **Mixins** add shared behaviour to multiple classes without an inheritance chain — implementing **composition over inheritance**.
```js
const Serializable = (Base) => class extends Base {
  serialize()   { return JSON.stringify(this); }
  static deserialize(json) { return Object.assign(new this(), JSON.parse(json)); }
};

const Timestamped = (Base) => class extends Base {
  createdAt = new Date();
  updatedAt = new Date();
  touch()   { this.updatedAt = new Date(); }
};

class Entity { constructor(id) { this.id = id; } }

class User extends Serializable(Timestamped(Entity)) {
  constructor(id, name) { super(id); this.name = name; }
}

const u = new User(1, 'Alice');
u.serialize(); // '{"id":1,"name":"Alice","createdAt":"..."}'
```

**Q56. What is method chaining?**
> Returning `this` from each method allows calling multiple methods in a single expression — a **fluent interface** pattern.
```js
class QueryBuilder {
  #table = ''; #conditions = []; #limit = null;

  from(table)      { this.#table = table;          return this; }
  where(condition) { this.#conditions.push(condition); return this; }
  limitTo(n)       { this.#limit = n;              return this; }

  build() {
    let q = `SELECT * FROM ${this.#table}`;
    if (this.#conditions.length) q += ` WHERE ${this.#conditions.join(' AND ')}`;
    if (this.#limit !== null)    q += ` LIMIT ${this.#limit}`;
    return q;
  }
}

new QueryBuilder()
  .from('users')
  .where('age > 18')
  .where('active = true')
  .limitTo(10)
  .build();
// 'SELECT * FROM users WHERE age > 18 AND active = true LIMIT 10'
```

**Q57. What is polymorphism in JavaScript?**
> The ability to call the same method on different objects and get different behaviours. In JS, achieved through overriding methods in subclasses or duck typing.
```js
class Shape  { area() { return 0; } }
class Circle extends Shape { constructor(r) { super(); this.r = r; } area() { return Math.PI * this.r ** 2; } }
class Rect   extends Shape { constructor(w,h) { super(); this.w=w; this.h=h; } area() { return this.w * this.h; } }

const shapes = [new Circle(5), new Rect(4, 6)];
shapes.forEach(s => console.log(s.area())); // 78.53, 24
// Same interface — different behaviour
```

**Q58. What is encapsulation?**
> Bundling data and the methods that operate on it into a single unit (class), and restricting direct access to internal state. In modern JS, achieved with private fields (`#`).
```js
class BankAccount {
  #balance;
  #transactions = [];

  constructor(initial) { this.#balance = initial; }

  deposit(amt) {
    if (amt <= 0) throw new Error('Amount must be positive');
    this.#balance += amt;
    this.#transactions.push({ type: 'deposit', amt });
  }

  get balance() { return this.#balance; } // read-only public accessor
}

const acc = new BankAccount(100);
acc.deposit(50);
acc.balance;   // 150
acc.#balance;  // SyntaxError — private field inaccessible
```

**Q59. What is `Object.create()` and when do you use it?**
> Creates a new object with a specified prototype. Useful for prototypal inheritance without classes, or creating objects with `null` prototype (no inherited properties — clean dictionaries).
```js
const proto = {
  greet() { return `Hello, I'm ${this.name}`; }
};
const obj = Object.create(proto);
obj.name = 'Alice';
obj.greet(); // 'Hello, I'm Alice'

// Null prototype — clean dictionary, no toString, hasOwnProperty etc.
const dict = Object.create(null);
dict.key = 'value';
'toString' in dict;  // false
```

**Q60. What is the difference between `call`, `apply`, and `bind`?**
> All three explicitly set `this`. `call` and `apply` invoke immediately; `bind` returns a new function.
```js
function introduce(lang, years) {
  return `I'm ${this.name}, I know ${lang} for ${years} years`;
}
const person = { name: 'Alice' };

introduce.call(person, 'JavaScript', 5);
// I'm Alice, I know JavaScript for 5 years

introduce.apply(person, ['JavaScript', 5]);
// same result — args as array

const boundIntro = introduce.bind(person, 'JavaScript');
boundIntro(5);
// same result — partially applied, called later

// Practical use: borrow methods
const arrayLike = { 0: 'a', 1: 'b', length: 2 };
Array.prototype.slice.call(arrayLike); // ['a', 'b']
```

---

> **Questions 46–60 complete.** Type `continue` for Questions 61–75.

---

## Questions 61–75 — Async JavaScript, Promises & Event Loop

**Q61. How does JavaScript handle asynchronous code despite being single-threaded?**
> JavaScript has one call stack — it can only do one thing at a time. Async operations (timers, network, I/O) are handed off to **Web APIs** (browser) or **libuv** (Node.js). When complete, their callbacks are added to the **task queue**. The **event loop** continuously checks: if the call stack is empty, it picks the next callback from the queue and pushes it onto the stack.
```
Call Stack      Web APIs        Task Queue       Microtask Queue
─────────       ────────        ──────────       ───────────────
main()   ──► setTimeout ──► callback added ──► Promise.then added
```

**Q62. What is the Event Loop?**
> The event loop is the mechanism that coordinates:
> 1. **Call stack** — currently executing code.
> 2. **Microtask queue** — Promise callbacks, `queueMicrotask`. Drained **completely** before the next task.
> 3. **Macrotask (task) queue** — `setTimeout`, `setInterval`, I/O. One task per event loop tick.
```js
console.log('1 - sync');
setTimeout(() => console.log('4 - macrotask'), 0);
Promise.resolve().then(() => console.log('3 - microtask'));
console.log('2 - sync');
// Output: 1, 2, 3, 4
```

**Q63. What is the difference between microtasks and macrotasks?**
> | Type       | Examples                                      | When processed           |
> |------------|-----------------------------------------------|--------------------------|
> | Microtask  | `Promise.then/catch/finally`, `queueMicrotask`, `MutationObserver` | After current task, before next macrotask — full queue drained |
> | Macrotask  | `setTimeout`, `setInterval`, `setImmediate`, I/O, UI events | One per event loop tick  |
```js
setTimeout(() => console.log('timeout'));      // macrotask
Promise.resolve().then(() => console.log('promise')); // microtask
queueMicrotask(() => console.log('microtask'));        // microtask
// Output: promise, microtask, timeout
```

**Q64. What is a callback and what is callback hell?**
> A callback is a function passed as an argument to be called when an async operation completes.
> **Callback hell** (pyramid of doom) — deeply nested callbacks that are hard to read, debug, and handle errors in.
```js
// Callback hell
getUser(id, (user) => {
  getPosts(user.id, (posts) => {
    getComments(posts[0].id, (comments) => {
      render(comments, (result) => {
        // ... deeper and deeper
      });
    });
  });
});

// Solution: Promises or async/await flatten the structure
const user     = await getUser(id);
const posts    = await getPosts(user.id);
const comments = await getComments(posts[0].id);
```

**Q65. What is a Promise?**
> A Promise is an object representing the eventual completion or failure of an async operation. It has three states: **pending**, **fulfilled**, **rejected**. Once settled, it is immutable — cannot change state again.
```js
const p = new Promise((resolve, reject) => {
  const success = true;
  success ? resolve('data') : reject(new Error('failed'));
});

p.then(data  => console.log(data))   // 'data'
 .catch(err  => console.error(err))
 .finally(() => console.log('done'));// always runs
```

**Q66. What is Promise chaining?**
> Each `.then()` returns a new Promise, allowing chaining. The return value of one `.then` becomes the input of the next. Errors propagate down to the nearest `.catch`.
```js
fetch('/api/user')
  .then(res  => res.json())            // parse JSON
  .then(user => fetch(`/api/posts/${user.id}`)) // use result
  .then(res  => res.json())
  .then(posts => console.log(posts))
  .catch(err => console.error(err))    // catches any error above
  .finally(() => hideSpinner());
```

**Q67. What are `Promise.all`, `Promise.allSettled`, `Promise.race`, and `Promise.any`?**
```js
const p1 = Promise.resolve(1);
const p2 = Promise.resolve(2);
const p3 = Promise.reject('err');

// all — fails fast on first rejection
Promise.all([p1, p2])         // [1, 2]
Promise.all([p1, p3])         // rejects with 'err'

// allSettled — waits for all, never rejects
Promise.allSettled([p1, p3])
// [{status:'fulfilled', value:1}, {status:'rejected', reason:'err'}]

// race — first to settle wins (fulfilled or rejected)
Promise.race([
  new Promise(r => setTimeout(() => r('slow'), 1000)),
  new Promise(r => setTimeout(() => r('fast'), 100)),
]) // 'fast'

// any — first to FULFILL wins; rejects only if ALL reject
Promise.any([p3, p1])  // 1
Promise.any([p3, p3])  // AggregateError: All promises were rejected
```

**Q68. What is `async/await`?**
> Syntactic sugar over Promises. `async` marks a function that always returns a Promise. `await` pauses execution inside the async function until a Promise settles — without blocking the thread.
```js
async function fetchUser(id) {
  try {
    const res  = await fetch(`/api/users/${id}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const user = await res.json();
    return user;
  } catch (err) {
    console.error('Fetch failed:', err);
    throw err; // re-throw or handle
  }
}

// Parallel — run concurrently, not sequentially
async function loadDashboard() {
  const [user, posts, stats] = await Promise.all([
    fetchUser(1),
    fetchPosts(),
    fetchStats(),
  ]);
}
```

**Q69. What are common async/await mistakes?**
> 1. **Forgetting `await`** — function returns a Promise, not the value.
> 2. **Sequential `await` in a loop** — each waits for the previous (slow).
> 3. **Using `forEach` with async** — `forEach` doesn't await Promises.
> 4. **Unhandled rejections** — no `try/catch` or `.catch()`.
> 5. **Blocking the thread** — putting synchronous heavy computation in an async function doesn't help.
```js
// WRONG — sequential, slow
for (const id of ids) {
  const user = await fetchUser(id); // waits for each
}

// RIGHT — parallel
const users = await Promise.all(ids.map(id => fetchUser(id)));

// WRONG — forEach doesn't await
ids.forEach(async id => { await fetchUser(id); }); // fire and forget

// RIGHT
for (const id of ids) { await fetchUser(id); } // if sequential needed
// or Promise.all for parallel
```

**Q70. What is a generator function?**
> A function that can pause its execution and resume later using `yield`. Returns an iterator. Control is passed back and forth between caller and generator.
```js
function* counter(start = 0) {
  while (true) {
    const reset = yield start++;
    if (reset) start = 0;
  }
}

const gen = counter(1);
gen.next();       // { value: 1, done: false }
gen.next();       // { value: 2, done: false }
gen.next(true);   // reset — { value: 0, done: false }

// Finite generator
function* range(start, end, step = 1) {
  for (let i = start; i < end; i += step) yield i;
}
[...range(0, 10, 2)]; // [0, 2, 4, 6, 8]
```

**Q71. What is the difference between `setTimeout(fn, 0)` and `Promise.resolve().then(fn)`?**
> Both are async — neither runs immediately. But they go into **different queues**:
> - `Promise.resolve().then(fn)` — **microtask queue** — runs before the next macrotask.
> - `setTimeout(fn, 0)` — **macrotask queue** — runs after all microtasks are drained.
```js
setTimeout(() => console.log('A - macrotask'), 0);
Promise.resolve().then(() => console.log('B - microtask'));
console.log('C - sync');
// Output: C, B, A
```

**Q72. What is `AbortController` and how is it used?**
> Allows cancelling async operations — primarily `fetch` requests. Signals can be shared across multiple requests.
```js
const controller = new AbortController();

fetch('/api/data', { signal: controller.signal })
  .then(res => res.json())
  .catch(err => {
    if (err.name === 'AbortError') console.log('Request cancelled');
    else throw err;
  });

// Cancel after 5 seconds
setTimeout(() => controller.abort(), 5000);

// Cancel on user action (e.g., component destroy in Angular)
ngOnDestroy() { this.controller.abort(); }
```

**Q73. What is `async` iteration and `for await...of`?**
> Used to iterate over async data sources (streams, paginated APIs) that emit values over time via async iterables.
```js
async function* paginate(url) {
  let page = 1;
  while (true) {
    const res  = await fetch(`${url}?page=${page}`);
    const data = await res.json();
    if (!data.items.length) return;
    yield data.items;
    page++;
  }
}

for await (const items of paginate('/api/products')) {
  console.log('Page of items:', items);
}
```

**Q74. What is the difference between error handling with `.catch()` vs `try/catch`?**
> `.catch()` is for Promise chains — attaches to a specific point in the chain, catches all errors above it.
> `try/catch` inside `async` functions — cleaner syntax, catches both sync throws and awaited Promise rejections in one block.
```js
// Promise chain style
doAsync()
  .then(processResult)
  .catch(handleError)    // catches errors from doAsync + processResult
  .finally(cleanup);

// async/await style
async function run() {
  try {
    const result = await doAsync();
    processResult(result);
  } catch (err) {
    handleError(err);   // catches both sync and async errors
  } finally {
    cleanup();
  }
}
```

**Q75. What is `queueMicrotask`?**
> Schedules a callback to run as a microtask — after the current synchronous code, before the next macrotask, alongside Promise callbacks. Useful for deferring work without creating a Promise.
```js
console.log('1');
queueMicrotask(() => console.log('3 - microtask'));
Promise.resolve().then(() => console.log('4 - microtask'));
setTimeout(() => console.log('5 - macrotask'), 0);
console.log('2');
// Output: 1, 2, 3, 4, 5
// Note: queueMicrotask and Promise microtasks run in insertion order
```

---

> **Questions 61–75 complete.** Type `continue` for Questions 76–90.

---

## Questions 76–90 — Advanced JavaScript (ES6+, Modules, Patterns, Performance)

**Q76. What are ES6 Modules and how do they differ from CommonJS?**
> | Feature           | ES Modules (ESM)              | CommonJS (CJS)              |
> |-------------------|-------------------------------|-----------------------------|
> | Syntax            | `import` / `export`           | `require()` / `module.exports` |
> | Loading           | Static, async                 | Dynamic, synchronous        |
> | Tree-shaking      | Yes — bundlers can remove unused exports | No             |
> | Live bindings     | Yes — imported values reflect updates | No — copied value    |
> | Top-level await   | Supported                     | Not supported               |
> | `this` at top level | `undefined`                 | `module.exports`            |
```js
// ESM
export const PI = 3.14;
export default function area(r) { return PI * r * r; }
import area, { PI } from './math.js';

// Dynamic import (lazy loading)
const { default: heavy } = await import('./heavy-module.js');
```

**Q77. What is tree shaking?**
> Tree shaking is dead-code elimination performed by bundlers (Webpack, Rollup, esbuild). It removes `export`ed code that is never `import`ed anywhere. Only works with static ESM — `require()` cannot be statically analysed.
```js
// math.js
export const add  = (a, b) => a + b;
export const mult = (a, b) => a * b; // never imported below

// main.js
import { add } from './math.js'; // bundler removes `mult` from output
```

**Q78. What is the Proxy object?**
> `Proxy` wraps an object and intercepts fundamental operations (get, set, delete, function call) via **traps**. Used for validation, logging, reactivity systems, and default values.
```js
const validator = {
  set(target, key, value) {
    if (key === 'age' && typeof value !== 'number')
      throw new TypeError('age must be a number');
    if (key === 'age' && value < 0)
      throw new RangeError('age cannot be negative');
    target[key] = value;
    return true; // must return true on success
  }
};

const person = new Proxy({}, validator);
person.name = 'Alice';  // OK
person.age  = 25;       // OK
person.age  = -1;       // RangeError
person.age  = 'old';    // TypeError
```

**Q79. What is `Reflect`?**
> `Reflect` is a built-in object that provides methods mirroring the Proxy trap names. It lets you call the default behaviour inside a trap without manual object manipulation — making Proxy handlers cleaner and more correct.
```js
const handler = {
  get(target, key, receiver) {
    console.log(`Getting: ${key}`);
    return Reflect.get(target, key, receiver); // default behaviour
  },
  set(target, key, value, receiver) {
    console.log(`Setting: ${key} = ${value}`);
    return Reflect.set(target, key, value, receiver);
  }
};

const obj = new Proxy({ x: 1 }, handler);
obj.x;     // logs "Getting: x", returns 1
obj.x = 2; // logs "Setting: x = 2"
```

**Q80. What is Symbol and when do you use it?**
> `Symbol` creates a unique, immutable primitive value. Every `Symbol()` call returns a guaranteed-unique value even with the same description. Used for unique property keys, well-known protocol hooks (`Symbol.iterator`, `Symbol.toPrimitive`), and meta-programming.
```js
const id = Symbol('id');
const obj = { [id]: 123, name: 'Alice' };
obj[id];                  // 123
Object.keys(obj);         // ['name'] — Symbol keys hidden
JSON.stringify(obj);      // '{"name":"Alice"}' — Symbol stripped

// Well-known Symbol — make object iterable
class Range {
  constructor(start, end) { this.start = start; this.end = end; }
  [Symbol.iterator]() {
    let current = this.start;
    const end   = this.end;
    return { next: () => current <= end
      ? { value: current++, done: false }
      : { done: true } };
  }
}
[...new Range(1, 5)]; // [1, 2, 3, 4, 5]
```

**Q81. What are Iterators and Iterables?**
> An **iterable** has a `[Symbol.iterator]()` method that returns an **iterator**. An iterator has a `next()` method that returns `{ value, done }`. `for...of`, spread `[...]`, and destructuring all consume iterables.
```js
// Custom iterable
const fibonacci = {
  [Symbol.iterator]() {
    let [a, b] = [0, 1];
    return {
      next() {
        if (a > 100) return { done: true };
        const value = a;
        [a, b] = [b, a + b];
        return { value, done: false };
      }
    };
  }
};

[...fibonacci]; // [0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89]
```

**Q82. What is the difference between `for...in` and `for...of`?**
> `for...in` — iterates over **enumerable property keys** (strings) of an object, including inherited ones. Use `Object.hasOwn` to filter. Avoid on arrays.
> `for...of` — iterates over **values** of any iterable (array, string, Map, Set, generator). Does not work on plain objects (not iterable by default).
```js
const arr = [10, 20, 30];
arr.custom = 'x';

for (const key of Object.keys(arr)) console.log(key);  // 0, 1, 2
for (const val of arr)              console.log(val);   // 10, 20, 30
for (const key in arr)              console.log(key);   // 0, 1, 2, custom ← includes custom prop

const map = new Map([['a', 1], ['b', 2]]);
for (const [k, v] of map) console.log(k, v); // a 1, b 2
```

**Q83. What is destructuring with default values and renaming?**
> Destructuring supports simultaneously renaming the variable AND providing a default value.
```js
// Object: rename + default
const { name: userName = 'Guest', age: userAge = 0 } = { name: 'Alice' };
// userName = 'Alice', userAge = 0

// Array: skip + default
const [, second = 99, third = 0] = [1];
// second = 99, third = 0

// Nested with default
const { address: { city = 'Unknown' } = {} } = {};
// city = 'Unknown' — safe even if address is missing
```

**Q84. What are tagged template literals?**
> A function called with the template's string parts and interpolated values. Used for sanitizing HTML, i18n, styled-components, GraphQL queries.
```js
function highlight(strings, ...values) {
  return strings.reduce((result, str, i) =>
    `${result}${str}${values[i] !== undefined ? `<mark>${values[i]}</mark>` : ''}`,
  '');
}

const name = 'Alice';
const score = 95;
highlight`Player ${name} scored ${score} points!`;
// 'Player <mark>Alice</mark> scored <mark>95</mark> points!'

// sql tag to prevent injection
const safe = sql`SELECT * FROM users WHERE id = ${userId}`;
```

**Q85. What is memoization and how do you implement it?**
> Caching a function's return value indexed by its arguments. On repeated identical calls, returns the cached result instead of recomputing. Best for pure, expensive functions.
```js
function memoize(fn) {
  const cache = new Map();
  return function(...args) {
    const key = JSON.stringify(args);
    if (cache.has(key)) return cache.get(key);
    const result = fn.apply(this, args);
    cache.set(key, result);
    return result;
  };
}

const expensiveFib = memoize(function fib(n) {
  if (n <= 1) return n;
  return expensiveFib(n - 1) + expensiveFib(n - 2);
});

expensiveFib(40); // computed once, then cached
expensiveFib(40); // instant from cache
```

**Q86. What is debounce and throttle?**
> **Debounce** — delays execution until N ms of inactivity. Only the last call in a burst runs.
> **Throttle** — guarantees execution at most once per N ms regardless of call frequency.
```js
// Debounce — search input
function debounce(fn, delay) {
  let timer;
  return function(...args) {
    clearTimeout(timer);
    timer = setTimeout(() => fn.apply(this, args), delay);
  };
}

// Throttle — scroll handler
function throttle(fn, interval) {
  let lastRun = 0;
  return function(...args) {
    const now = Date.now();
    if (now - lastRun >= interval) {
      lastRun = now;
      fn.apply(this, args);
    }
  };
}
```

**Q87. What are Web Workers?**
> Web Workers run JavaScript in a background thread — separate from the main thread. They cannot access the DOM. Communication is via `postMessage` / `onmessage`. Used for CPU-intensive tasks (sorting, parsing, compression) to keep the UI responsive.
```js
// worker.js
self.onmessage = function(e) {
  const result = heavyComputation(e.data);
  self.postMessage(result);
};

// main.js
const worker = new Worker('worker.js');
worker.postMessage(largeDataset);
worker.onmessage = e => console.log('Result:', e.data);
worker.onerror   = e => console.error(e);

// Terminate when done
worker.terminate();
```

**Q88. What is `structuredClone`?**
> A global function (ES2022) that creates a deep clone of a value using the structured clone algorithm. Handles `Date`, `Map`, `Set`, `ArrayBuffer`, `RegExp`, circular references. Does NOT handle functions, DOM nodes, or `Error` objects.
```js
const original = {
  date: new Date(),
  map:  new Map([['a', 1]]),
  set:  new Set([1, 2, 3]),
  arr:  [1, [2, [3]]],
};

const clone = structuredClone(original);
clone.date === original.date; // false — new Date object
clone.map  === original.map;  // false — new Map
clone.arr[1][0] = 99;
original.arr[1][0]; // 2 — independent

// Does NOT handle:
structuredClone(() => {});   // DataCloneError
structuredClone(document.body); // DataCloneError
```

**Q89. What is the difference between `Object.assign` and the spread operator for objects?**
> Both perform **shallow** merges. Differences:
> - `Object.assign` copies property descriptors' **values** (not descriptors themselves), triggers setters on the target.
> - Spread `{...src}` does not trigger setters, always creates a new object.
> - `Object.assign` can merge into an existing object; spread always creates a new one.
```js
const target = { a: 1 };
const source = { b: 2, c: 3 };

Object.assign(target, source); // target is mutated: { a:1, b:2, c:3 }
const merged = { ...target, ...source }; // new object, target unchanged

// Note: both are shallow
const obj = { nested: { x: 1 } };
const copy1 = Object.assign({}, obj);
const copy2 = { ...obj };
copy1.nested === obj.nested; // true — same reference (shallow)
copy2.nested === obj.nested; // true — same reference (shallow)
```

**Q90. What are design patterns? Name the most important ones in JavaScript.**
> Design patterns are reusable solutions to common software design problems. Key patterns for JavaScript:
> | Category   | Pattern       | Use case                                          |
> |------------|---------------|---------------------------------------------------|
> | Creational | Singleton     | One instance (config, store, connection pool)     |
> | Creational | Factory       | Create objects without `new` at call site         |
> | Creational | Builder       | Step-by-step complex object construction          |
> | Structural | Decorator     | Add behaviour dynamically (middleware, pipes)     |
> | Structural | Proxy         | Intercept access (validation, caching, logging)   |
> | Structural | Facade        | Simplified API over complex subsystem             |
> | Behavioural| Observer      | Pub/sub, event emitter, RxJS                      |
> | Behavioural| Strategy      | Swap algorithms at runtime (sort strategies)      |
> | Behavioural| Command       | Encapsulate actions (undo/redo, Redux dispatch)   |
> | Behavioural| Iterator      | Traverse collections (generators, `for...of`)     |
```js
// Observer in JS
class EventEmitter {
  #listeners = new Map();
  on(event, fn)   { (this.#listeners.get(event) ?? this.#listeners.set(event, new Set()).get(event)).add(fn); }
  off(event, fn)  { this.#listeners.get(event)?.delete(fn); }
  emit(event, ...args) { this.#listeners.get(event)?.forEach(fn => fn(...args)); }
}
```

---

> **Questions 76–90 complete.** Type `continue` for Questions 91–120.

---

## Questions 91–120 — Expert Level: Memory, Security, Patterns & Deep JS

**Q91. What is garbage collection in JavaScript?**
> The JS engine automatically reclaims memory that is no longer reachable. V8 uses a **generational garbage collector**: most objects die young (minor GC — "Scavenge") and are collected quickly. Long-lived objects are promoted to the old generation (major GC — "Mark-Sweep-Compact"). You cannot trigger GC manually. Avoid memory leaks by releasing references in `ngOnDestroy`, clearing timers, and removing event listeners.
```js
// Memory leak — listener never removed
window.addEventListener('resize', heavyHandler); // holds ref forever

// Fix
const handler = () => heavyComputation();
window.addEventListener('resize', handler);
// Later:
window.removeEventListener('resize', handler);
```

**Q92. What is a memory leak and what causes them in JavaScript?**
> A memory leak occurs when memory is allocated but never released because references are still held unintentionally. Common causes:
> 1. **Forgotten event listeners** — not removed on component destroy.
> 2. **Uncancelled subscriptions** (RxJS, setInterval).
> 3. **Closures holding large objects** inadvertently.
> 4. **Detached DOM nodes** still referenced in JS variables.
> 5. **Global variables** — grow unboundedly.
> 6. **Caches without eviction** — Maps growing without cleanup.
```js
// Leak: detached DOM node held in closure
let leakyCache = [];
document.getElementById('btn').addEventListener('click', () => {
  const node = document.createElement('div');
  leakyCache.push(node); // node removed from DOM but still referenced
});
```

**Q93. What is `WeakRef` and `FinalizationRegistry`?**
> `WeakRef` holds a weak reference to an object — it does not prevent GC. `FinalizationRegistry` lets you register a callback when an object is GC'd. Both are low-level memory management tools — use only when necessary (caches, pools).
```js
let target = { data: 'heavy' };
const weakRef = new WeakRef(target);

// Later — target may or may not have been collected
const obj = weakRef.deref(); // returns object or undefined
if (obj) console.log(obj.data);

// FinalizationRegistry
const registry = new FinalizationRegistry((label) => {
  console.log(`${label} was garbage collected`);
});
registry.register(target, 'heavy-object');
target = null; // eligible for GC — callback fires eventually
```

**Q94. What is the difference between `==` with objects and primitives?**
> With primitives, `==` coerces types then compares values. With objects, `==` and `===` both compare **references** — two objects are only equal if they point to the same location in memory, regardless of having identical content.
```js
const a = { x: 1 };
const b = { x: 1 };
const c = a;

a == b;   // false — different references
a === b;  // false — different references
a == c;   // true  — same reference
a === c;  // true  — same reference

// Type coercion with objects — calls valueOf/toString
[] == false;    // true  ([] → '' → 0, false → 0)
[] == ![];      // true  (![] = false, []==false = true) — famous gotcha
{} == {};       // false — different references
```

**Q95. What is `Object.defineProperty`?**
> Defines or modifies a property with full control over its descriptor: `value`, `writable`, `enumerable`, `configurable`, or accessor descriptors (`get`/`set`).
```js
const obj = {};
Object.defineProperty(obj, 'PI', {
  value: 3.14159,
  writable: false,       // cannot be changed
  enumerable: true,      // shows in for...in / Object.keys
  configurable: false,   // descriptor cannot be changed/deleted
});

obj.PI = 99;             // silently fails (strict mode throws)
console.log(obj.PI);     // 3.14159

// Accessor descriptor
Object.defineProperty(obj, 'now', {
  get() { return Date.now(); },
  enumerable: true,
  configurable: true,
});
obj.now; // current timestamp, computed fresh each access
```

**Q96. What is function composition and how does it differ from piping?**
> Both combine functions. **Composition** applies right-to-left: `compose(f, g)(x) = f(g(x))`. **Pipe** applies left-to-right: `pipe(f, g)(x) = g(f(x))`. Pipe reads in execution order — preferred in most codebases.
```js
const compose = (...fns) => x => fns.reduceRight((v, f) => f(v), x);
const pipe    = (...fns) => x => fns.reduce((v, f) => f(v), x);

const double  = x => x * 2;
const addOne  = x => x + 1;
const square  = x => x * x;

compose(square, addOne, double)(3); // square(addOne(double(3))) = 49
pipe(double, addOne, square)(3);    // square(addOne(double(3))) = 49 — same here
pipe(double, square, addOne)(3);    // addOne(square(double(3))) = 37 — order matters
```

**Q97. What is tail call optimisation (TCO)?**
> If the very last operation of a function is a recursive call (tail position), the engine can reuse the current stack frame instead of creating a new one — preventing stack overflow for deep recursion. ES6 specifies TCO in strict mode, but only Safari implements it fully.
```js
// NOT tail recursive — stack grows: result = n * factorial(n-1)
function factorial(n) {
  if (n <= 1) return 1;
  return n * factorial(n - 1); // multiplication after recursive call
}

// Tail recursive — accumulator carries result
function factorialTCO(n, acc = 1) {
  if (n <= 1) return acc;
  return factorialTCO(n - 1, n * acc); // last operation IS the recursive call
}
```

**Q98. What are pure functions?**
> A function is pure if: (1) given the same inputs it always returns the same output, and (2) it produces no side effects. Pure functions are predictable, testable, memoizable, and parallelisable.
```js
// Pure
const add = (a, b) => a + b;
const toUpper = s => s.toUpperCase();

// Impure — depends on external state
let multiplier = 3;
const scale = x => x * multiplier; // depends on external var

// Impure — side effect
function pushAll(arr, items) {
  arr.push(...items); // mutates argument
  return arr;
}

// Pure equivalent
const pushAll = (arr, items) => [...arr, ...items]; // new array
```

**Q99. What is immutability and why does it matter?**
> Immutability means data is never changed after creation — modifications produce new values. Benefits: predictable state, safe sharing across references, enables `OnPush` change detection, time-travel debugging, and easier reasoning about data flow.
```js
// Mutable (dangerous)
function addUser(users, user) {
  users.push(user);  // mutates original — unexpected side effects
  return users;
}

// Immutable (safe)
const addUser = (users, user) => [...users, user]; // new array

// Object update
const updateAge = (user, age) => ({ ...user, age }); // new object

// Nested update
const setCity = (user, city) => ({
  ...user,
  address: { ...user.address, city }
});
```

**Q100. What is the difference between imperative and declarative programming in JavaScript?**
> **Imperative**: describes **how** to do something — explicit control flow, mutations, step-by-step.
> **Declarative**: describes **what** you want — the engine/framework handles the how.
```js
const numbers = [1, 2, 3, 4, 5];

// Imperative
const evens = [];
for (let i = 0; i < numbers.length; i++) {
  if (numbers[i] % 2 === 0) evens.push(numbers[i]);
}

// Declarative
const evens = numbers.filter(n => n % 2 === 0);

// React/Angular templates are declarative — describe the desired UI,
// framework handles DOM manipulation imperatively under the hood.
```

**Q101. What is the difference between `throw` and `return` in error handling?**
> `return` exits the function with a value — caller must check the value for errors (error-as-value pattern). `throw` interrupts normal execution and unwinds the call stack until a `catch` block is found — used for exceptional, unexpected conditions.
```js
// Error-as-value (no throw)
function divide(a, b) {
  if (b === 0) return { ok: false, error: 'Division by zero' };
  return { ok: true, value: a / b };
}
const result = divide(10, 0);
if (!result.ok) handleError(result.error);

// Exception (throw)
function divide(a, b) {
  if (b === 0) throw new RangeError('Division by zero');
  return a / b;
}
try { divide(10, 0); } catch (e) { handleError(e); }
```

**Q102. What are the different types of errors in JavaScript?**
> | Error Type       | When thrown                                          |
> |------------------|------------------------------------------------------|
> | `Error`          | Generic base class                                   |
> | `TypeError`      | Wrong type used (call non-function, access null prop)|
> | `ReferenceError` | Undeclared variable accessed                         |
> | `SyntaxError`    | Invalid syntax (parse time)                          |
> | `RangeError`     | Value out of allowed range (array length, recursion) |
> | `URIError`       | Invalid `encodeURI`/`decodeURI` argument             |
> | `EvalError`      | Issues with `eval()` (rare)                          |
> | `AggregateError` | Multiple errors (Promise.any all rejected)           |
```js
// Always throw Error instances (not strings) for proper stack traces
throw new TypeError('Expected a string');    // correct
throw 'Expected a string';                  // avoid — no stack trace

// Custom error
class ValidationError extends Error {
  constructor(field, message) {
    super(message);
    this.name = 'ValidationError';
    this.field = field;
  }
}
throw new ValidationError('email', 'Invalid format');
```

**Q103. What is `eval()` and why should you avoid it?**
> `eval()` executes a string as JavaScript code. Avoid because:
> 1. **Security**: executes arbitrary code — XSS vulnerability if input is user-controlled.
> 2. **Performance**: prevents V8 optimisations (disables JIT for the surrounding function).
> 3. **Debugging**: errors are hard to trace.
> 4. **Scope pollution**: in non-strict mode, `eval` can create variables in the enclosing scope.
```js
// NEVER do this
const userInput = 'alert("XSS!")';
eval(userInput); // dangerous!

// Alternative: JSON.parse for data, Function constructor if unavoidable
const safeData = JSON.parse('{"a":1}'); // safe object creation
```

**Q104. What is the `with` statement and why is it deprecated?**
> `with` adds an object to the scope chain, so its properties can be accessed without qualification. Forbidden in strict mode because it makes scope impossible to statically analyse — confuses engines and developers. Never use it.
```js
// 'with' — DO NOT USE
with (Math) {
  console.log(sqrt(16)); // doesn't need Math.sqrt
}
// Better:
const { sqrt, PI } = Math;
console.log(sqrt(16));
```

**Q105. What is strict mode and what does it enable?**
> Strict mode (`'use strict'`) opts into a restricted variant of JavaScript:
> 1. Prevents accidental global variable creation.
> 2. Assignment to read-only properties throws (instead of silently failing).
> 3. Duplicate parameter names are a syntax error.
> 4. `this` is `undefined` in regular functions (not the global object).
> 5. `with` is forbidden.
> 6. `eval` cannot create variables in surrounding scope.
> ES6 modules and classes are always in strict mode automatically.
```js
'use strict';
undeclared = 5;      // ReferenceError — would create global without strict
function f() { return this; }
f();                 // undefined (not window)
```

**Q106. What is `Object.keys` vs `Reflect.ownKeys`?**
> `Object.keys()` — returns own, **enumerable**, **string** keys only.
> `Object.getOwnPropertyNames()` — returns own, **all** string keys (including non-enumerable).
> `Reflect.ownKeys()` — returns own, **all** string AND Symbol keys (most complete).
```js
const sym = Symbol('id');
const obj = Object.defineProperty({ [sym]: 1, name: 'Alice' }, 'hidden', {
  value: 'secret', enumerable: false
});

Object.keys(obj);                 // ['name']
Object.getOwnPropertyNames(obj);  // ['name', 'hidden']
Reflect.ownKeys(obj);             // ['name', 'hidden', Symbol(id)]
```

**Q107. What is the logical assignment operators (`&&=`, `||=`, `??=`)?**
> ES2021 short-circuit assignment operators — only assign if the condition is truthy/falsy/nullish.
```js
let a = null, b = 0, c = 1;

a ??= 'default';  // a = 'default' (was nullish)
b ||= 42;         // b = 42        (was falsy)
c &&= 99;         // c = 99        (was truthy)

// Equivalent long forms
a = a ?? 'default';
b = b || 42;
c = c && 99;

// Practical: initialise cache entry
cache.users ??= [];
cache.users.push(newUser);
```

**Q108. What is the `in` operator used for beyond property checks?**
> `in` also works with `for...in` loops and — as of ES2022 — can check **private class fields** inside a class.
```js
'toString' in {};      // true — inherited property
0 in [1, 2, 3];        // true — index 0 exists
'length' in [];        // true

// Private field check (ES2022)
class Node {
  #value;
  constructor(v) { this.#value = v; }
  static isNode(obj) { return #value in obj; } // checks private field
}
Node.isNode(new Node(1)); // true
Node.isNode({});          // false
```

**Q109. What are the `Array.from` and `Array.of` methods?**
> `Array.from(iterable, mapFn)` — creates an array from any iterable or array-like object, with optional transform.
> `Array.of(...values)` — creates an array from arguments regardless of count (fixes `new Array(3)` ambiguity).
```js
Array.from('hello');              // ['h','e','l','l','o']
Array.from({ length: 3 }, (_, i) => i * 2); // [0, 2, 4]
Array.from(new Set([1, 2, 3]));   // [1, 2, 3]
Array.from(new Map([['a',1]]));   // [['a', 1]]

Array.of(3);      // [3]    — one element
new Array(3);     // [,,]   — sparse array of length 3 (confusing!)
Array.of(1,2,3);  // [1, 2, 3]
```

**Q110. What is `structuredClone` vs JSON serialisation vs `lodash.cloneDeep`?**
> | Method                          | Handles `Date`/`Map`/`Set` | Circular refs | Functions | Speed  |
> |---------------------------------|---------------------------|---------------|-----------|--------|
> | `JSON.parse(JSON.stringify())` | No (Date → string, Map/Set lost) | No (throws) | No | Fast |
> | `structuredClone`              | Yes                        | Yes           | No        | Fast   |
> | `lodash.cloneDeep`             | Yes                        | Yes           | No        | Slower |
```js
const d = new Date();
JSON.parse(JSON.stringify({ d })).d;  // string — lost Date type
structuredClone({ d }).d;            // Date object — preserved
structuredClone({ fn: () => {} });   // DataCloneError — no functions
```

**Q111. What is the difference between `Array.prototype.sort` being stable vs unstable?**
> ES2019 mandated that `Array.prototype.sort` must be **stable** — elements with equal sort keys preserve their original relative order. Before this, some engines used unstable algorithms for arrays > 10 elements.
```js
const items = [
  { name: 'Bob',   age: 25 },
  { name: 'Alice', age: 25 },
  { name: 'Carol', age: 30 },
];
items.sort((a, b) => a.age - b.age);
// Stable: Bob and Alice stay in original order (Bob before Alice)
// [Bob(25), Alice(25), Carol(30)] — Bob still before Alice
```

**Q112. What is `Promise.withResolvers()` (ES2024)?**
> Returns an object `{ promise, resolve, reject }` — lets you resolve/reject a Promise from outside its constructor. Previously required awkward variable extraction from the `new Promise` constructor.
```js
// Before ES2024 — awkward
let resolve, reject;
const promise = new Promise((res, rej) => { resolve = res; reject = rej; });

// ES2024
const { promise, resolve, reject } = Promise.withResolvers();

// External control
button.addEventListener('click', () => resolve('clicked'));
setTimeout(() => reject(new Error('timeout')), 5000);
await promise;
```

**Q113. What is `Object.groupBy` and `Map.groupBy` (ES2024)?**
> Native grouping methods replacing common `reduce` patterns.
```js
const people = [
  { name: 'Alice', dept: 'eng' },
  { name: 'Bob',   dept: 'hr'  },
  { name: 'Carol', dept: 'eng' },
];

const byDept = Object.groupBy(people, p => p.dept);
// { eng: [Alice, Carol], hr: [Bob] }

// Map.groupBy — for non-string keys
const byAge = Map.groupBy(people, p => p.name.length);
```

**Q114. What is the `using` declaration (ES2025 / Stage 4)?**
> Provides **explicit resource management** — automatically calls `[Symbol.dispose]()` on a value when the block exits (similar to `using` in C# / `with` in Python). `await using` for async resources.
```js
class TempFile {
  constructor(path) { this.path = path; fs.writeFileSync(path, ''); }
  [Symbol.dispose]() { fs.rmSync(this.path); } // cleanup
}

{
  using file = new TempFile('/tmp/work.txt');
  fs.writeFileSync(file.path, 'data');
  // file is automatically deleted when block exits
}
```

**Q115. What is `Array.prototype.at()` and why is it useful?**
> `.at(index)` supports negative indices for accessing elements from the end — without the `arr[arr.length - 1]` verbosity. Also works on strings and TypedArrays.
```js
const arr = [1, 2, 3, 4, 5];
arr.at(0);     // 1  — first
arr.at(-1);    // 5  — last
arr.at(-2);    // 4  — second from end

'hello'.at(-1); // 'o' — last character

// Before .at():
arr[arr.length - 1]; // verbose and error-prone
```

**Q116. How does JavaScript handle integer precision limits?**
> JavaScript uses 64-bit IEEE 754 floats. The safe integer range is `Number.MIN_SAFE_INTEGER` (`-2^53 + 1`) to `Number.MAX_SAFE_INTEGER` (`2^53 - 1`). Beyond this, arithmetic is imprecise. Use `BigInt` for arbitrary precision integers.
```js
Number.MAX_SAFE_INTEGER;          // 9007199254740991
9007199254740992 === 9007199254740993; // true! — precision lost

// BigInt — append n or use BigInt()
const big = 9007199254740992n;
big + 1n;  // 9007199254740993n — precise
big + 1;   // TypeError — cannot mix BigInt and Number
Number(big); // 9007199254740992 — back to float (loses precision)
```

**Q117. What is the Temporal API (Stage 3 proposal)?**
> The Temporal API is the modern replacement for the broken `Date` object. It provides immutable, timezone-aware date/time classes with a clear, unambiguous API. Not yet in all environments — use polyfill `@js-temporal/polyfill`.
```js
// Temporal (future standard)
const now  = Temporal.Now.plainDateTimeISO();
const date = Temporal.PlainDate.from('2026-01-15');
date.add({ months: 1 }).toString(); // '2026-02-15'

// vs old Date — ambiguous timezone, mutable, confusing months (0-indexed)
new Date(2026, 0, 15); // January 15 — 0-indexed month is a trap
```

**Q118. What is the difference between `for...of` and `.forEach` with `async`?**
> `for...of` with `await` truly waits for each iteration. `forEach` ignores returned Promises — async callbacks run concurrently and unhandled rejections may be silently swallowed.
```js
const ids = [1, 2, 3];

// WRONG — forEach doesn't await, all fire simultaneously
ids.forEach(async id => {
  await processItem(id); // Promise returned but ignored by forEach
});

// Sequential (await each)
for (const id of ids) {
  await processItem(id); // truly waits
}

// Parallel (all at once, await all)
await Promise.all(ids.map(id => processItem(id)));
```

**Q119. What are the main JavaScript engine optimisations you should know?**
> V8 (Chrome/Node.js) key optimisations:
> 1. **JIT compilation** — converts hot JS to optimised machine code.
> 2. **Hidden classes (Shapes)** — objects with the same property order share a hidden class. Always initialise all properties in the constructor in the same order.
> 3. **Inline caches** — V8 caches property access locations. Monomorphic (one shape) access is fastest.
> 4. **Deoptimisation** — changing an object's shape or a function's argument types forces V8 to throw away optimised code and re-interpret.
```js
// Optimal — consistent shape
class Point {
  constructor(x, y) { this.x = x; this.y = y; } // always x then y
}

// Suboptimal — different shapes
const p1 = { x: 1, y: 2 };
const p2 = { y: 2, x: 1 };  // different property order → different hidden class
```

**Q120. What are the key JavaScript security best practices?**
> 1. **Never use `eval()`** or `new Function(userInput)` — XSS vulnerability.
> 2. **Sanitize all HTML output** — use `textContent` not `innerHTML` for user data.
> 3. **Use `Content-Security-Policy`** headers to prevent script injection.
> 4. **Avoid `document.write()`** — blocks parsing, can be exploited.
> 5. **Use `HttpOnly` cookies** for tokens — inaccessible to JS.
> 6. **Validate on the server** — client-side validation is UI-only, never security.
> 7. **Use `SameSite` cookies** to prevent CSRF.
> 8. **Avoid prototype pollution** — check `Object.hasOwn` not `in` for user-supplied keys.
```js
// XSS — WRONG
element.innerHTML = userInput;  // executes <script> tags

// XSS — CORRECT
element.textContent = userInput; // renders as text, never executes

// Prototype pollution — WRONG
function merge(target, source) {
  for (const key in source) target[key] = source[key]; // allows __proto__ pollution
}

// Safe merge
function safeMerge(target, source) {
  for (const key of Object.keys(source)) {
    if (Object.hasOwn(target, key) || Object.hasOwn(source, key))
      target[key] = source[key];
  }
}
```

---

> **All 120 JavaScript Interview Questions complete.**

---

# Logical Programming Questions (30 Questions)

> These are coding challenges — read the problem, solve it yourself first, then check the solution. Problems range from beginner logic to advanced algorithm design.

---

## Logical Questions 1–15

**LP1. Reverse a string without using `.reverse()`**
```js
// Solution 1: spread + reduce
const reverse = s => [...s].reduce((acc, c) => c + acc, '');

// Solution 2: two-pointer
function reverse(s) {
  const arr = [...s];
  let l = 0, r = arr.length - 1;
  while (l < r) { [arr[l], arr[r]] = [arr[r], arr[l]]; l++; r--; }
  return arr.join('');
}

reverse('hello');    // 'olleh'
reverse('racecar');  // 'racecar'
```

**LP2. Check if a string is a palindrome**
```js
// Clean functional approach
const isPalindrome = s => {
  const clean = s.toLowerCase().replace(/[^a-z0-9]/g, '');
  return clean === [...clean].reverse().join('');
};

isPalindrome('racecar');             // true
isPalindrome('A man a plan a canal Panama'); // true
isPalindrome('hello');               // false
```

**LP3. Find the factorial of a number (iterative & recursive)**
```js
// Recursive
const factorial = n => n <= 1 ? 1 : n * factorial(n - 1);

// Iterative
function factorial(n) {
  let result = 1;
  for (let i = 2; i <= n; i++) result *= i;
  return result;
}

// Tail-recursive
const factorial = (n, acc = 1) => n <= 1 ? acc : factorial(n - 1, n * acc);

factorial(5);  // 120
factorial(0);  // 1
```

**LP4. Generate Fibonacci sequence up to N terms**
```js
function fibonacci(n) {
  if (n <= 0) return [];
  if (n === 1) return [0];
  const seq = [0, 1];
  for (let i = 2; i < n; i++) seq.push(seq[i-1] + seq[i-2]);
  return seq;
}

fibonacci(8); // [0, 1, 1, 2, 3, 5, 8, 13]

// Generator version
function* fib() {
  let [a, b] = [0, 1];
  while (true) { yield a; [a, b] = [b, a + b]; }
}
const gen = fib();
Array.from({ length: 8 }, () => gen.next().value); // [0,1,1,2,3,5,8,13]
```

**LP5. Find the largest and smallest numbers in an array**
```js
const numbers = [3, 1, 7, 2, 9, 4];

// O(n) single pass
function minMax(arr) {
  let min = Infinity, max = -Infinity;
  for (const n of arr) {
    if (n < min) min = n;
    if (n > max) max = n;
  }
  return { min, max };
}

// One-liner
const max = Math.max(...numbers); // 9
const min = Math.min(...numbers); // 1

// Caution: spread blows stack for very large arrays → use reduce
const max2 = numbers.reduce((m, n) => n > m ? n : m, -Infinity);
```

**LP6. Check if a number is prime**
```js
function isPrime(n) {
  if (n < 2) return false;
  if (n === 2) return true;
  if (n % 2 === 0) return false;
  for (let i = 3; i <= Math.sqrt(n); i += 2) {
    if (n % i === 0) return false;
  }
  return true;
}

isPrime(2);  // true
isPrime(7);  // true
isPrime(9);  // false (3×3)
isPrime(1);  // false
```

**LP7. Find all duplicate values in an array**
```js
function findDuplicates(arr) {
  const seen = new Set();
  const dupes = new Set();
  for (const item of arr) {
    if (seen.has(item)) dupes.add(item);
    else seen.add(item);
  }
  return [...dupes];
}

findDuplicates([1, 2, 3, 2, 4, 3, 5]); // [2, 3]
```

**LP8. Flatten a deeply nested array**
```js
// Recursive
function flatten(arr) {
  return arr.reduce((acc, val) =>
    Array.isArray(val) ? acc.concat(flatten(val)) : [...acc, val], []);
}

// Native
[1, [2, [3, [4]]]].flat(Infinity); // [1, 2, 3, 4]

// Stack-based (iterative, avoids call stack overflow for very deep nesting)
function flatten(arr) {
  const stack = [...arr], result = [];
  while (stack.length) {
    const val = stack.pop();
    Array.isArray(val) ? stack.push(...val) : result.unshift(val);
  }
  return result;
}
```

**LP9. Count the frequency of each character in a string**
```js
function charFrequency(str) {
  return [...str].reduce((freq, char) => {
    freq[char] = (freq[char] ?? 0) + 1;
    return freq;
  }, {});
}

charFrequency('hello');
// { h:1, e:1, l:2, o:1 }

// With Map
function charFrequencyMap(str) {
  const map = new Map();
  for (const c of str) map.set(c, (map.get(c) ?? 0) + 1);
  return map;
}
```

**LP10. Find the first non-repeating character in a string**
```js
function firstUniqueChar(s) {
  const freq = new Map();
  for (const c of s) freq.set(c, (freq.get(c) ?? 0) + 1);
  for (const c of s) if (freq.get(c) === 1) return c;
  return null;
}

firstUniqueChar('leetcode');  // 'l'
firstUniqueChar('aabb');      // null
firstUniqueChar('loveleet');  // 'v'
```

**LP11. Implement a stack using an array**
```js
class Stack {
  #items = [];

  push(item)   { this.#items.push(item); }
  pop()        { if (this.isEmpty()) throw new Error('Stack underflow'); return this.#items.pop(); }
  peek()       { return this.#items[this.#items.length - 1]; }
  isEmpty()    { return this.#items.length === 0; }
  get size()   { return this.#items.length; }
  toArray()    { return [...this.#items]; }
}

const s = new Stack();
s.push(1); s.push(2); s.push(3);
s.peek();  // 3
s.pop();   // 3
s.size;    // 2
```

**LP12. Implement a queue using two stacks**
```js
class QueueFromStacks {
  #inbox  = [];
  #outbox = [];

  enqueue(item) { this.#inbox.push(item); }

  dequeue() {
    if (!this.#outbox.length) {
      while (this.#inbox.length) this.#outbox.push(this.#inbox.pop());
    }
    if (!this.#outbox.length) throw new Error('Queue is empty');
    return this.#outbox.pop();
  }

  get size() { return this.#inbox.length + this.#outbox.length; }
}

const q = new QueueFromStacks();
q.enqueue(1); q.enqueue(2); q.enqueue(3);
q.dequeue(); // 1 (FIFO)
q.dequeue(); // 2
```

**LP13. Check if parentheses/brackets are balanced**
```js
function isBalanced(s) {
  const pairs = { ')': '(', ']': '[', '}': '{' };
  const stack = [];
  for (const c of s) {
    if ('([{'.includes(c)) stack.push(c);
    else if (')]}'.includes(c)) {
      if (stack.pop() !== pairs[c]) return false;
    }
  }
  return stack.length === 0;
}

isBalanced('({[]})');  // true
isBalanced('({[})');   // false
isBalanced('((()'));   // false
```

**LP14. Find the missing number in an array from 1 to N**
```js
// Using sum formula: expected - actual = missing
function missingNumber(arr) {
  const n      = arr.length + 1; // expected N if one is missing
  const expected = (n * (n + 1)) / 2;
  const actual   = arr.reduce((sum, n) => sum + n, 0);
  return expected - actual;
}

missingNumber([1, 2, 4, 5, 6]); // 3
missingNumber([1, 3, 4, 5]);    // 2

// XOR approach — O(n), O(1) space
function missingNumber(arr) {
  let xor = arr.length;
  for (let i = 0; i < arr.length; i++) xor ^= i ^ arr[i];
  return xor;
}
```

**LP15. Rotate an array by K positions**
```js
function rotateRight(arr, k) {
  const n = arr.length;
  k = k % n; // handle k > n
  return [...arr.slice(n - k), ...arr.slice(0, n - k)];
}

// In-place with reversals — O(n) time, O(1) space
function rotateRight(arr, k) {
  const n = arr.length;
  k = ((k % n) + n) % n; // handle negative k
  reverse(arr, 0, n - 1);
  reverse(arr, 0, k - 1);
  reverse(arr, k, n - 1);
  return arr;
}
function reverse(arr, l, r) {
  while (l < r) { [arr[l], arr[r]] = [arr[r], arr[l]]; l++; r--; }
}

rotateRight([1,2,3,4,5], 2); // [4,5,1,2,3]
```

---

## Logical Questions 16–30

**LP16. Find the two numbers in an array that sum to a target**
```js
// O(n) with a Set
function twoSum(arr, target) {
  const seen = new Set();
  for (const n of arr) {
    const complement = target - n;
    if (seen.has(complement)) return [complement, n];
    seen.add(n);
  }
  return null;
}

twoSum([2, 7, 11, 15], 9);  // [2, 7]
twoSum([3, 2, 4], 6);       // [2, 4]

// Return indices (LeetCode style)
function twoSumIndices(arr, target) {
  const map = new Map();
  for (let i = 0; i < arr.length; i++) {
    const comp = target - arr[i];
    if (map.has(comp)) return [map.get(comp), i];
    map.set(arr[i], i);
  }
}
```

**LP17. Find the maximum subarray sum (Kadane's Algorithm)**
```js
function maxSubarraySum(arr) {
  let maxSum = arr[0];
  let current = arr[0];
  for (let i = 1; i < arr.length; i++) {
    current = Math.max(arr[i], current + arr[i]);
    maxSum  = Math.max(maxSum, current);
  }
  return maxSum;
}

maxSubarraySum([-2, 1, -3, 4, -1, 2, 1, -5, 4]); // 6 (subarray: [4,-1,2,1])
maxSubarraySum([-1, -2, -3]);                      // -1 (least negative)
```

**LP18. Group anagrams together**
```js
function groupAnagrams(words) {
  const map = new Map();
  for (const word of words) {
    const key = [...word].sort().join('');
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(word);
  }
  return [...map.values()];
}

groupAnagrams(['eat','tea','tan','ate','nat','bat']);
// [['eat','tea','ate'], ['tan','nat'], ['bat']]
```

**LP19. Implement binary search**
```js
function binarySearch(arr, target) {
  let lo = 0, hi = arr.length - 1;
  while (lo <= hi) {
    const mid = lo + Math.floor((hi - lo) / 2); // avoid integer overflow
    if      (arr[mid] === target) return mid;
    else if (arr[mid] <  target)  lo = mid + 1;
    else                           hi = mid - 1;
  }
  return -1; // not found
}

binarySearch([1,3,5,7,9,11,13], 7);  // 3 (index)
binarySearch([1,3,5,7,9,11,13], 6);  // -1
```

**LP20. Deep clone an object without `structuredClone` or JSON**
```js
function deepClone(value) {
  if (value === null || typeof value !== 'object') return value;
  if (value instanceof Date)   return new Date(value.getTime());
  if (value instanceof RegExp) return new RegExp(value.source, value.flags);
  if (value instanceof Map)    return new Map([...value].map(([k,v]) => [deepClone(k), deepClone(v)]));
  if (value instanceof Set)    return new Set([...value].map(deepClone));
  if (Array.isArray(value))    return value.map(deepClone);
  return Object.fromEntries(
    Object.entries(value).map(([k, v]) => [k, deepClone(v)])
  );
}

const obj = { a: 1, b: { c: new Date(), d: [1, 2] } };
const clone = deepClone(obj);
clone.b.c === obj.b.c; // false — deep clone
```

**LP21. Implement `Array.prototype.reduce` from scratch**
```js
function myReduce(arr, callback, initialValue) {
  let accumulator;
  let startIndex;

  if (arguments.length >= 3) {
    accumulator = initialValue;
    startIndex  = 0;
  } else {
    if (arr.length === 0) throw new TypeError('Reduce of empty array with no initial value');
    accumulator = arr[0];
    startIndex  = 1;
  }

  for (let i = startIndex; i < arr.length; i++) {
    accumulator = callback(accumulator, arr[i], i, arr);
  }
  return accumulator;
}

myReduce([1,2,3,4], (acc, n) => acc + n, 0); // 10
myReduce([1,2,3],   (acc, n) => acc * n);    // 6
```

**LP22. Find the longest common prefix in an array of strings**
```js
function longestCommonPrefix(words) {
  if (!words.length) return '';
  let prefix = words[0];
  for (let i = 1; i < words.length; i++) {
    while (!words[i].startsWith(prefix)) {
      prefix = prefix.slice(0, -1);
      if (!prefix) return '';
    }
  }
  return prefix;
}

longestCommonPrefix(['flower', 'flow', 'flight']); // 'fl'
longestCommonPrefix(['dog', 'racecar', 'car']);     // ''
longestCommonPrefix(['interview', 'interact', 'internal']); // 'inter'
```

**LP23. Implement a `pipe` function that supports async functions**
```js
const asyncPipe = (...fns) => x =>
  fns.reduce(async (promise, fn) => fn(await promise), Promise.resolve(x));

// Usage
const fetchUser   = async id   => ({ id, name: 'Alice' });
const addRole     = async user => ({ ...user, role: 'admin' });
const formatUser  = async user => `${user.name} (${user.role})`;

const getFormattedUser = asyncPipe(fetchUser, addRole, formatUser);
await getFormattedUser(1); // 'Alice (admin)'
```

**LP24. Find the kth largest element in an unsorted array**
```js
// O(n log n) — sort
function kthLargest(arr, k) {
  return arr.slice().sort((a, b) => b - a)[k - 1];
}

// O(n) average — QuickSelect (partition)
function kthLargest(arr, k) {
  return quickSelect([...arr], 0, arr.length - 1, arr.length - k);
}
function quickSelect(arr, lo, hi, k) {
  if (lo === hi) return arr[lo];
  const pivot = partition(arr, lo, hi);
  if      (pivot === k) return arr[pivot];
  else if (pivot  < k)  return quickSelect(arr, pivot + 1, hi, k);
  else                   return quickSelect(arr, lo, pivot - 1, k);
}
function partition(arr, lo, hi) {
  const pivot = arr[hi];
  let i = lo;
  for (let j = lo; j < hi; j++) {
    if (arr[j] <= pivot) { [arr[i], arr[j]] = [arr[j], arr[i]]; i++; }
  }
  [arr[i], arr[hi]] = [arr[hi], arr[i]];
  return i;
}

kthLargest([3, 2, 1, 5, 6, 4], 2); // 5
```

**LP25. Implement debounce with immediate option**
```js
function debounce(fn, delay, { immediate = false } = {}) {
  let timer;
  return function(...args) {
    const callNow = immediate && !timer;
    clearTimeout(timer);
    timer = setTimeout(() => {
      timer = null;
      if (!immediate) fn.apply(this, args);
    }, delay);
    if (callNow) fn.apply(this, args);
  };
}

// immediate: true — fires on leading edge
const saveImmediate = debounce(save, 300, { immediate: true });
// immediate: false (default) — fires after burst ends
const searchDelayed = debounce(search, 300);
```

**LP26. Implement a `once` function — runs a callback only once**
```js
function once(fn) {
  let called = false;
  let result;
  return function(...args) {
    if (!called) {
      called = true;
      result = fn.apply(this, args);
    }
    return result; // returns same value on subsequent calls
  };
}

const init = once(() => { console.log('initialised'); return 42; });
init(); // logs 'initialised', returns 42
init(); // silent, returns 42
init(); // silent, returns 42
```

**LP27. Implement a `compose` function that works with async and sync functions**
```js
const compose = (...fns) => fns.reduce(
  (f, g) => async (...args) => f(await g(...args))
);

const trim   = s => s.trim();
const upper  = s => s.toUpperCase();
const fetchData = async id => `  data-${id}  `;

const processData = compose(upper, trim, fetchData);
await processData(5); // 'DATA-5'
```

**LP28. Find all permutations of a string**
```js
function permutations(str) {
  if (str.length <= 1) return [str];
  const result = new Set();
  for (let i = 0; i < str.length; i++) {
    const char = str[i];
    const rest = str.slice(0, i) + str.slice(i + 1);
    for (const perm of permutations(rest)) {
      result.add(char + perm);
    }
  }
  return [...result];
}

permutations('abc'); // ['abc','acb','bac','bca','cab','cba']
permutations('aab'); // ['aab','aba','baa'] — Set deduplicates
```

**LP29. Implement a rate limiter — allow at most N calls per T milliseconds**
```js
function createRateLimiter(maxCalls, windowMs) {
  const timestamps = [];
  return function(fn, ...args) {
    const now = Date.now();
    // Remove timestamps outside the window
    while (timestamps.length && timestamps[0] <= now - windowMs) timestamps.shift();
    if (timestamps.length >= maxCalls) {
      const waitMs = windowMs - (now - timestamps[0]);
      console.warn(`Rate limit exceeded. Retry in ${waitMs}ms`);
      return null;
    }
    timestamps.push(now);
    return fn(...args);
  };
}

const limiter = createRateLimiter(3, 1000); // 3 calls per second
limiter(apiCall, data1); // OK
limiter(apiCall, data2); // OK
limiter(apiCall, data3); // OK
limiter(apiCall, data4); // Rate limit exceeded
```

**LP30. Implement a Promise pool — run at most N Promises concurrently**
```js
async function promisePool(tasks, concurrency) {
  const results = [];
  let index = 0;

  async function worker() {
    while (index < tasks.length) {
      const taskIndex = index++;
      results[taskIndex] = await tasks[taskIndex]();
    }
  }

  const workers = Array.from({ length: Math.min(concurrency, tasks.length) }, worker);
  await Promise.all(workers);
  return results;
}

// Usage: download 10 files but only 3 at a time
const tasks = urls.map(url => () => fetch(url).then(r => r.json()));
const results = await promisePool(tasks, 3);
```

---

> **All 30 Logical Programming Questions complete.**
>
> **The JavaScript-Mastery-for-Angular.md file is now fully complete** — 15 Modules + 120 Interview Questions + 30 Logical Challenges.

*End of JavaScript-Mastery-for-Angular.md*
