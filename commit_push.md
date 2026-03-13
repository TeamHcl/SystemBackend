# Commit & Push Workflow

This document defines the **mandatory Git workflow** for all contributors and agents working in this repository.
The goal is to maintain a **stable `master` branch**, clean commit history, and predictable collaboration.

---

# 1. Branching Policy (STRICT)

The `master` branch is the **production/stable branch**.

**Direct pushes to `master` are strictly prohibited.**

All development must be done in **individual branches**.

Example branches:

```
harinivelraj
madhumitha
ruthresh
```

### Required workflow

1. Work in your **own branch**
2. Push commits to **your branch**
3. Open a **Pull Request (PR)**
4. Merge PR into **master**

```
your-branch → master
```

---

# 2. Mandatory Sync Before Starting Work

Before beginning any work session, always fetch and update from `master`.

```
git fetch origin
git checkout master
git pull origin master
```

Then switch back to your branch:

```
git checkout <your-branch>
git merge master
```

If conflicts occur:

* Resolve them locally
* Test the project
* Commit the resolution

This ensures your branch stays **compatible with the latest master state**.

---

# 3. Standard Development Workflow

Follow this sequence every time you work on the repository.

### Step 1 — Fetch Latest Repository State

```
git fetch origin
```

---

### Step 2 — Update Local Master

```
git checkout master
git pull origin master
```

---

### Step 3 — Switch to Your Branch

```
git checkout <your-branch>
```

---

### Step 4 — Merge Latest Master Into Your Branch

```
git merge master
```

Resolve conflicts if they appear.

---

### Step 5 — Implement Changes

Make your code changes locally.

---

### Step 6 — Stage and Commit Changes

```
git add .
git commit -m "type(scope): message"
```

Commit messages must follow **Conventional Commit format**.

---

### Step 7 — Push to Your Branch

```
git push origin <your-branch>
```

---

### Step 8 — Open Pull Request

Create a Pull Request on GitHub:

```
<your-branch> → master
```

Your PR must:

* Be conflict-free
* Contain clear commits
* Pass review

---

# 4. Keeping Your PR Mergeable

If `master` changes after your PR is created, update your branch:

```
git fetch origin
git checkout master
git pull origin master
git checkout <your-branch>
git merge master
```

Resolve conflicts if needed.

Then push again:

```
git push origin <your-branch>
```

Your Pull Request will update automatically.

---

# 5. Commit Message Standard (Conventional Commits)

All commits **must follow the Conventional Commits format**:

```
type(scope): message
```

Example structure:

```
type(scope): short-description
```

---

## Allowed Commit Types

| Type     | Purpose                              |
| -------- | ------------------------------------ |
| feat     | New feature                          |
| fix      | Bug fix                              |
| docs     | Documentation changes                |
| style    | Formatting changes (no logic change) |
| refactor | Code restructuring                   |
| perf     | Performance improvements             |
| test     | Tests added or modified              |
| build    | Build system or dependency updates   |
| ci       | CI/CD configuration changes          |
| chore    | Maintenance tasks                    |

---

## Good Commit Examples

```
feat(auth): add JWT authentication middleware
fix(api): resolve null pointer in user service
docs(readme): update installation guide
refactor(database): optimize query performance
test(agent): add conversation handler tests
```

---

## Bad Commit Examples (Not Allowed)

```
update
changes
fix
misc
final
```

These commits are unclear and will be rejected during review.

---

## Commit Message Rules

* Use **present tense**
* Keep message **short and descriptive**
* Prefer **≤ 72 characters**
* Split large work into **multiple logical commits**

Example:

```
git commit -m "feat(agent): implement context memory handler"
```

---

# 6. Pull Request Guidelines

Every Pull Request must include:

* Clear description of changes
* Clean commit messages
* No merge conflicts
* Working code

PRs may be rejected if:

* Branch is outdated with `master`
* Commit messages are unclear
* Code is broken or incomplete

---

# 7. Forbidden Actions

The following actions are **strictly prohibited**:

```
❌ Direct push to master
❌ Force push to shared branches
❌ Skipping pull request process
❌ Large unreviewed commits
❌ Ignoring merge conflicts
```

Violating these rules may lead to **restricted repository access**.

---

# 8. Quick Workflow Summary

```
git fetch origin
git checkout master
git pull origin master

git checkout <your-branch>
git merge master

# implement changes

git add .
git commit -m "type(scope): message"

git push origin <your-branch>

# open PR → master
```

---

# 9. Responsibility

Each contributor is responsible for:

* Keeping their branch updated
* Resolving merge conflicts
* Writing meaningful commits
* Ensuring their PR is merge-ready

The `master` branch must **always remain stable and deployable**.
