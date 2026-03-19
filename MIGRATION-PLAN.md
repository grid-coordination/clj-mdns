# Migration Plan: mdns → clj-mdns (grid-coordination)

Tracked as beads issue OA3T-eij in ~/projects/grid/repo/clj-oa3-test

## Goal
Migrate this personal mDNS library to the grid-coordination GitHub org
for use as a dependency of clj-oa3-client (OpenADR 3 VTN discovery via mDNS).

## Current State
- 2 commits, on `feature/all-services` branch
- Uncommitted: modified `client.clj`, untracked `instance.clj`, `LICENSE`, `CHANGELOG.md`, `client-BEFORE.clj`
- Remote: `github:dcj/mDNS` (personal)
- Maven coords: `com.dcj/mdns` with stale `aircraft-noise` repo refs
- Deps: jmdns 3.6.0, commons-lang3, medley

## Steps

### 1. Clean up git state
- Delete `client-BEFORE.clj` (it's the pre-feature-branch version, no longer needed)
- Commit `instance.clj`, `LICENSE`, `CHANGELOG.md`, and modified `client.clj`
- Merge feature branch to main, delete `develop` and `feature/all-services` branches

### 2. Update project metadata
- Rename maven coords to `energy.grid-coordination/clj-mdns`
- Remove `aircraft-noise` maven repo references from deps.edn
- Remove old depstar/deps-deploy aliases (or update to current tooling)
- Update Clojure version to 1.12.3

### 3. Create new GitHub repo and push
- Create `grid-coordination/clj-mdns` on GitHub
- Update git remote from `dcj/mDNS` to `grid-coordination/clj-mdns`
- Push main

### 4. Move local directory
- Move from `~/projects/mdns` to `~/projects/grid/repo/clj-mdns`

### 5. Apply shadow-repo pattern
- Run `/shadow-repo-init` to create shadow repo with CLAUDE.md, beads, etc.

### 6. Verify
- Start nREPL, test basic mDNS discovery works
- Ensure clj-oa3-client can reference it as `{:local/root "../clj-mdns"}`

## Context
- The grid-coordination org convention is `energy.grid-coordination` for maven group-id
- Other repos in the org: clj-oa3, clj-oa3-client, clj-oa3-test
- All use `:local/root` references between sibling repos under `~/projects/grid/repo/`
- Shadow-repo pattern docs: `~/etc/shadow-repo-pattern.md`
- Shadow-repo init skill: `~/.claude/commands/shadow-repo-init.md`

## After This
OA3T-2k9 (blocked by this) adds mDNS discovery to clj-oa3-client.
