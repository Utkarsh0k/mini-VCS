# Core - Mini Version Control System

A lightweight educational Version Control System built in Java to understand the internal architecture of systems like Git.

This project focuses on the fundamental concepts behind version control systems including:

* content-addressed storage
* commit hashing
* branch pointers
* parent-linked commit history
* snapshot restoration

The goal of this project is not to fully replicate Git, but to implement the core data model used internally by modern VCS systems.

---

# Features

* Repository initialization
* Commit creation
* SHA-256 based commit identity
* Immutable commit storage
* Commit history traversal
* Branch creation
* Branch switching
* Snapshot restoration
* File tracking system
* Global command-line usage

---

# Repository Structure

After initialization, the repository structure looks like:

```text id="7mk9fo"
.core/
│
├── objects/
│
├── refs/
│   └── main
│
└── HEAD
```

## Structure Explanation

### objects/

Stores:

* commit objects
* tracked file contents

Each object is stored using its SHA-256 hash as filename.

---

### refs/

Stores branch pointers.

Example:

```text id="7j62gi"
refs/main
```

Contains:

```text id="mnmjlwm"
2fc95925cc5b885...
```

which points to the latest commit.

---

### HEAD

Stores the currently active branch.

Example:

```text id="2af6xu"
main
```

---

# Commit Architecture

Each commit stores:

```text id="vvcg4m"
parent:<parentHash>
author:Utkarsh
date:<timestamp>
message:<commitMessage>

files:
<filename> <fileHash>
```

Commits are immutable because the commit ID is generated directly from commit content.

---

# Commit Graph Model

Commits form a parent-linked history chain:

```text id="n4mwlo"
C3 -> C2 -> C1 -> null
```

Each commit stores the hash of its parent commit, allowing reconstruction of repository history.

---

# Branch Model

Branches are implemented as movable pointers.

Example:

```text id="ixb9uw"
main -> C3
dev  -> C3
```

After divergence:

```text id="h1n43z"
main -> C4
dev  -> C3
```

The active branch is determined by the `HEAD` file.

---

# Hashing System

The system uses SHA-256 hashing through Java's `MessageDigest`.

Hash generation process:

1. Convert content to UTF-8 bytes
2. Apply SHA-256 hashing
3. Convert hash bytes into hexadecimal string
4. Use resulting hash as object identifier

This ensures object identity is content-based.

---

# Commands

## INIT

### Description

Initialize a new repository in current directory.

### Syntax

```bash id="e1vhq1"
core init
```

---

## COMMIT

### Description

Create a new commit snapshot.

### Syntax

```bash id="ecbbvl"
core commit "commit message"
```

---

## LOG

### Description

Display commit history.

### Syntax

```bash id="8g1ee8"
core log
```

---

## BRANCH

### Description

Create a new branch.

### Syntax

```bash id="j7t7xv"
core branch <branch-name>
```

---

## BRANCHES

### Description

List all available branches.

### Syntax

```bash id="2c76od"
core branches
```

---

## CHECKOUT

### Description

Switch active branch and restore branch snapshot.

### Syntax

```bash id="y5hck0"
core checkout <branch-name>
```

---

## RESTORE

### Description

Restore repository files from a specific commit snapshot.

### Syntax

```bash id="y4ml8q"
core restore <commit-id>
```

---

## STATUS

### Description

Display current repository status.

### Syntax

```bash id="ax0ee6"
core status
```

---

## HELP

### Description

Show help menu.

### Syntax

```bash id="l7u9cw"
core help
```

---

# Example Workflow

## Initialize repository

```bash id="vseccf"
core init
```

---

## Create first commit

```bash id="k8r7vu"
core commit "Initial Commit"
```

---

## Create branch

```bash id="h7d9s0"
core branch dev
```

---

## Switch branch

```bash id="z4of9o"
core checkout dev
```

---

## View history

```bash id="y4ck0l"
core log
```

---

## Restore old snapshot

```bash id="jxf9ae"
core restore a1b2c3
```

---

# Current Limitations

This project intentionally keeps the implementation minimal.

Currently unsupported features include:

* merge
* diff
* staging area
* conflict resolution
* remote repositories
* file deletion tracking
* binary file optimization

The focus is on understanding the core internal model of version control systems.

---

# Educational Objective

The primary goal of this project is to explore how version control systems internally manage:

* commit graphs
* immutable objects
* branch references
* snapshot restoration
* content hashing

This project demonstrates the foundational architecture behind systems like Git in a simplified and educational form.

---

# Technologies Used

* Java
* SHA-256 (`MessageDigest`)
* File-based object storage
* Command-line interface

---

# Author

Utkarsh
