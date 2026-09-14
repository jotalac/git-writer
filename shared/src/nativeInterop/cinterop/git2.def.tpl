# cinterop binding for libgit2 (iosMain). Slices come from tools/build-libgit2.sh.
# ${libgit2LibPath} is substituted per target by shared/build.gradle.kts.

headers = git2.h
headerFilter = git2.h git2/*.h
package = git2
compilerOpts = -DGIT_DEPRECATE_HARD=0
staticLibraries = libgit2.a
libraryPaths = ${libgit2LibPath}

---
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <git2/sys/errors.h>

/* Shims exist because cinterop cannot translate brace-initializer macros
 * (KT-43717) or call variadic functions, and maps bitmask enums to Kotlin enum
 * classes that cannot be used as out-params. Shims never hand heap memory to
 * Kotlin: variably sized output goes into a caller buffer. All are static. */

#define GITW_PATH_MAX 1024
#define GITW_NAME_MAX 256

/* Errors */

/* GIT_BUF_INIT is a brace macro and has no init function. */
static inline void gitw_buf_init(git_buf *buf) {
    memset(buf, 0, sizeof(git_buf));
}

/* Thread-local, never NULL, and only valid immediately after a failure. */
static inline const char *gitw_error_message(void) {
    const git_error *e = git_error_last();
    return e ? e->message : "";
}

/* libgit2 error class (GIT_ERROR_*), to avoid matching on messages. */
static inline int gitw_error_class(void) {
    const git_error *e = git_error_last();
    return e ? e->klass : 0;
}

/* Credentials */

/* Named callback type so Kotlin can pass the right signature. */
typedef git_credential_acquire_cb gitw_cred_cb;

/* git_credential is opaque; libgit2 duplicates both strings. */
static inline int gitw_cred_userpass(git_credential **out, const char *username, const char *password) {
    return git_credential_userpass_plaintext_new(out, username, password);
}

/* Repository / branch helpers */

/* Current branch shorthand into a caller buffer; -1 when detached or unborn. */
static inline int gitw_current_branch(git_repository *repo, char *out, size_t out_len) {
    git_reference *head = NULL;
    if (git_repository_head(&head, repo) < 0) return -1;
    const char *name = git_reference_shorthand(head);
    if (!name) { git_reference_free(head); return -1; }
    snprintf(out, out_len, "%s", name);
    git_reference_free(head);
    return 0;
}

/* 1 while a merge is in progress. State is the only reliable signal: an
 * already-resolved merge still has MERGE_HEAD on disk. */
static inline int gitw_merge_in_progress(git_repository *repo) {
    return git_repository_state(repo) == GIT_REPOSITORY_STATE_MERGE ? 1 : 0;
}

/* 1 when dirty (incl. untracked), 0 when clean, < 0 on error. */
static inline int gitw_is_dirty(git_repository *repo) {
    git_status_options opts;
    if (git_status_options_init(&opts, GIT_STATUS_OPTIONS_VERSION) < 0) return -1;
    opts.show = GIT_STATUS_SHOW_INDEX_AND_WORKDIR;
    opts.flags = GIT_STATUS_OPT_INCLUDE_UNTRACKED | GIT_STATUS_OPT_RECURSE_UNTRACKED_DIRS;
    git_status_list *list = NULL;
    if (git_status_list_new(&list, repo, &opts) < 0) return -1;
    int n = (int)git_status_list_entrycount(list);
    git_status_list_free(list);
    return n > 0 ? 1 : 0;
}

/* 1 when the index has conflicts. git_index_has_conflicts is a boolean. */
static inline int gitw_index_conflict_count(git_repository *repo) {
    git_index *index = NULL;
    if (git_repository_index(&index, repo) < 0) return -1;
    int n = git_index_has_conflicts(index) ? 1 : 0;
    git_index_free(index);
    return n;
}

/* Staging and committing */

/* Author == committer; falls back when user.name/email are unset. */
static inline int gitw_signature_or_default(git_signature **out, git_repository *repo) {
    if (git_signature_default(out, repo) == 0) return 0;
    git_error_clear();
    return git_signature_now(out, "Git Writer", "gitwriter@localhost");
}

/* Stage everything, including deletions (git add -A followed by git add -u). */
static inline int gitw_add_all(git_repository *repo) {
    git_index *index = NULL;
    if (git_repository_index(&index, repo) < 0) return -1;
    int rc = git_index_add_all(index, NULL, GIT_INDEX_ADD_DEFAULT, NULL, NULL);
    if (rc == 0) rc = git_index_update_all(index, NULL, NULL, NULL);
    if (rc == 0) rc = git_index_write(index);
    git_index_free(index);
    return rc;
}

/* Commit the staged tree with HEAD as parent. git_commit_create's first
 * parameter is an out-param: NULL fails and leaves the commit dangling. */
static inline int gitw_commit_staged(git_repository *repo, const char *message) {
    git_index *index = NULL;
    git_tree *tree = NULL;
    git_commit *parent = NULL;
    git_signature *sig = NULL;
    git_oid tree_oid, head_oid, commit_oid;
    int rc = git_repository_index(&index, repo);
    if (rc == 0) rc = git_index_write_tree(&tree_oid, index);
    if (rc == 0) rc = git_tree_lookup(&tree, repo, &tree_oid);
    if (rc == 0) rc = gitw_signature_or_default(&sig, repo);
    if (rc == 0) {
        int parents = 0;
        if (git_reference_name_to_id(&head_oid, repo, "HEAD") == 0) {
            if (git_commit_lookup(&parent, repo, &head_oid) == 0) parents = 1;
        } else {
            git_error_clear();
        }
        rc = git_commit_create(&commit_oid, repo, "HEAD", sig, sig, NULL, message, tree, parents,
                               parents ? (const git_commit **)&parent : NULL);
    }
    if (sig) git_signature_free(sig);
    if (parent) git_commit_free(parent);
    if (tree) git_tree_free(tree);
    if (index) git_index_free(index);
    return rc;
}

/* Commit only when dirty: 1 committed, 0 nothing to do, < 0 failure. */
static inline int gitw_commit_if_dirty(git_repository *repo, const char *message) {
    int dirty = gitw_is_dirty(repo);
    if (dirty < 0) return -1;
    if (dirty == 0) return 0;
    if (gitw_add_all(repo) < 0) return -1;
    return gitw_commit_staged(repo, message) < 0 ? -1 : 1;
}

/* Commit a merge of HEAD and MERGE_HEAD (two parents). */
static inline int gitw_commit_merge(git_repository *repo, const char *message) {
    git_index *index = NULL;
    git_tree *tree = NULL;
    git_signature *sig = NULL;
    git_commit *first = NULL, *second = NULL;
    git_oid tree_oid, oid, commit_oid;
    int rc = git_repository_index(&index, repo);
    if (rc == 0) rc = git_index_write_tree(&tree_oid, index);
    if (rc == 0) rc = git_tree_lookup(&tree, repo, &tree_oid);
    if (rc == 0) rc = gitw_signature_or_default(&sig, repo);
    if (rc == 0) {
        const git_commit *parents[2];
        int n = 0;
        if (git_reference_name_to_id(&oid, repo, "HEAD") == 0 && git_commit_lookup(&first, repo, &oid) == 0)
            parents[n++] = first;
        if (git_reference_name_to_id(&oid, repo, "MERGE_HEAD") == 0 && git_commit_lookup(&second, repo, &oid) == 0)
            parents[n++] = second;
        rc = git_commit_create(&commit_oid, repo, "HEAD", sig, sig, NULL, message, tree, n, n ? parents : NULL);
    }
    if (first) git_commit_free(first);
    if (second) git_commit_free(second);
    if (sig) git_signature_free(sig);
    if (tree) git_tree_free(tree);
    if (index) git_index_free(index);
    return rc;
}

/* Hard reset to HEAD, discarding working-tree and index changes. */
static inline int gitw_reset_hard(git_repository *repo) {
    return git_reset(repo, NULL, GIT_RESET_HARD, NULL);
}

/* Conflict resolution: GIT_CHECKOUT_USE_OURS/THEIRS do not work, because
 * checkout refuses an index with unmerged entries. Per path: read the chosen
 * stage's blob, write it to the worktree, conflict_remove, then add_bypath. */

/* OURS = stage 2. */
static inline int gitw_resolve_conflicts_ours(git_repository *repo, const char *only_path) {
    git_index *index = NULL;
    if (git_repository_index(&index, repo) < 0) return -1;
    int rounds = 0, result = 0;
    while (git_index_has_conflicts(index) && rounds++ < 100000) {
        git_index_conflict_iterator *it = NULL;
        if (git_index_conflict_iterator_new(&it, index) < 0) { result = -1; break; }
        const git_index_entry *ancestor = NULL, *our = NULL, *their = NULL;
        int nrc = git_index_conflict_next(&ancestor, &our, &their, it);
        git_index_conflict_iterator_free(it);
        if (nrc != 0) break;
        const git_index_entry *any = our ? our : (their ? their : ancestor);
        if (!any) break;
        char *path = strdup(any->path);
        if (!path) { result = -1; break; }
        if (only_path && strcmp(only_path, path) != 0) {
            /* Not the requested file: drop just its conflict entry so the
               iterator makes progress, but leave the file untouched. */
            git_index_conflict_remove(index, path);
            git_index_write(index);
            git_index_read(index, 1);
            free(path);
            continue;
        }
        if (!our) {
            /* Ours deleted the file. */
            git_index_remove(index, path, 0);
            const char *wd = git_repository_workdir(repo);
            if (wd) { char full[GITW_PATH_MAX]; snprintf(full, sizeof(full), "%s%s", wd, path); remove(full); }
            git_index_write(index);
            git_index_read(index, 1);
            free(path);
            continue;
        }
        git_blob *blob = NULL;
        if (git_blob_lookup(&blob, repo, &our->id) < 0) { free(path); result = -1; break; }
        const void *raw = git_blob_rawcontent(blob);
        size_t len = (size_t)git_blob_rawsize(blob);
        const char *wd = git_repository_workdir(repo);
        char full[GITW_PATH_MAX];
        snprintf(full, sizeof(full), "%s%s", wd ? wd : "", path);
        FILE *fh = fopen(full, "wb");
        if (!fh) { git_blob_free(blob); free(path); result = -1; break; }
        if (len) fwrite(raw, 1, len, fh);
        fclose(fh);
        git_blob_free(blob);
        git_index_conflict_remove(index, path);
        git_index_read(index, 1);
        if (git_index_add_bypath(index, path) < 0) { free(path); result = -1; break; }
        git_index_write(index);
        git_index_read(index, 1);
        free(path);
    }
    git_index_free(index);
    return result;
}

/* THEIRS = stage 3. */
static inline int gitw_resolve_conflicts_theirs(git_repository *repo, const char *only_path) {
    git_index *index = NULL;
    if (git_repository_index(&index, repo) < 0) return -1;
    int rounds = 0, result = 0;
    while (git_index_has_conflicts(index) && rounds++ < 100000) {
        git_index_conflict_iterator *it = NULL;
        if (git_index_conflict_iterator_new(&it, index) < 0) { result = -1; break; }
        const git_index_entry *ancestor = NULL, *our = NULL, *their = NULL;
        int nrc = git_index_conflict_next(&ancestor, &our, &their, it);
        git_index_conflict_iterator_free(it);
        if (nrc != 0) break;
        const git_index_entry *any = our ? our : (their ? their : ancestor);
        if (!any) break;
        char *path = strdup(any->path);
        if (!path) { result = -1; break; }
        if (only_path && strcmp(only_path, path) != 0) {
            git_index_conflict_remove(index, path);
            git_index_write(index);
            git_index_read(index, 1);
            free(path);
            continue;
        }
        if (!their) {
            /* Theirs deleted the file. */
            git_index_remove(index, path, 0);
            const char *wd = git_repository_workdir(repo);
            if (wd) { char full[GITW_PATH_MAX]; snprintf(full, sizeof(full), "%s%s", wd, path); remove(full); }
            git_index_write(index);
            git_index_read(index, 1);
            free(path);
            continue;
        }
        git_blob *blob = NULL;
        if (git_blob_lookup(&blob, repo, &their->id) < 0) { free(path); result = -1; break; }
        const void *raw = git_blob_rawcontent(blob);
        size_t len = (size_t)git_blob_rawsize(blob);
        const char *wd = git_repository_workdir(repo);
        char full[GITW_PATH_MAX];
        snprintf(full, sizeof(full), "%s%s", wd ? wd : "", path);
        FILE *fh = fopen(full, "wb");
        if (!fh) { git_blob_free(blob); free(path); result = -1; break; }
        if (len) fwrite(raw, 1, len, fh);
        fclose(fh);
        git_blob_free(blob);
        git_index_conflict_remove(index, path);
        git_index_read(index, 1);
        if (git_index_add_bypath(index, path) < 0) { free(path); result = -1; break; }
        git_index_write(index);
        git_index_read(index, 1);
        free(path);
    }
    git_index_free(index);
    return result;
}

/* Conflicted paths: 0 + path per entry, GIT_ITEROVER when done. */
static inline int gitw_conflict_iterator_new(git_index_conflict_iterator **out, git_repository *repo) {
    git_index *index = NULL;
    int rc = git_repository_index(&index, repo);
    if (rc == 0) rc = git_index_conflict_iterator_new(out, index);
    if (index) git_index_free(index);
    return rc;
}
static inline int gitw_conflict_next_path(git_index_conflict_iterator *it, char *out, size_t out_len) {
    const git_index_entry *a = NULL, *o = NULL, *t = NULL;
    int rc = git_index_conflict_next(&a, &o, &t, it);
    if (rc != 0) return rc;
    const git_index_entry *e = o ? o : (t ? t : a);
    if (!e) return GIT_ERROR;
    snprintf(out, out_len, "%s", e->path);
    return 0;
}

/* Remotes */

/* First configured remote, or "" when none. */
static inline int gitw_remote_list_first(git_repository *repo, char *out, size_t out_len) {
    git_strarray arr;
    if (git_remote_list(&arr, repo) < 0) return -1;
    if (arr.count > 0 && arr.strings && arr.strings[0]) {
        snprintf(out, out_len, "%s", arr.strings[0]);
    } else {
        out[0] = '\0';
    }
    git_strarray_dispose(&arr);
    return 0;
}

static inline int gitw_remote_create(git_repository *repo, const char *name, const char *url) {
    git_remote *r = NULL;
    int rc = git_remote_create(&r, repo, name, url);
    if (r) git_remote_free(r);
    return rc;
}

static inline int gitw_remote_set_url(git_repository *repo, const char *name, const char *url) {
    return git_remote_set_url(repo, name, url);
}

static inline int gitw_remote_delete(git_repository *repo, const char *name) {
    return git_remote_delete(repo, name);
}

/* Branch upstream config */

static inline int gitw_branch_set_upstream(git_repository *repo, const char *remote) {
    char branch[GITW_NAME_MAX];
    if (gitw_current_branch(repo, branch, sizeof(branch)) < 0) return -1;
    git_config *cfg = NULL;
    int rc = git_repository_config(&cfg, repo);
    char key[GITW_NAME_MAX * 2], val[GITW_NAME_MAX];
    if (rc == 0) {
        snprintf(key, sizeof(key), "branch.%s.remote", branch);
        rc = git_config_set_string(cfg, key, remote);
    }
    if (rc == 0) {
        snprintf(key, sizeof(key), "branch.%s.merge", branch);
        snprintf(val, sizeof(val), "refs/heads/%s", branch);
        rc = git_config_set_string(cfg, key, val);
    }
    if (cfg) git_config_free(cfg);
    return rc;
}

static inline int gitw_branch_unset_upstream(git_repository *repo) {
    char branch[GITW_NAME_MAX];
    if (gitw_current_branch(repo, branch, sizeof(branch)) < 0) return 0;
    git_config *cfg = NULL;
    if (git_repository_config(&cfg, repo) == 0) {
        char key[GITW_NAME_MAX * 2];
        snprintf(key, sizeof(key), "branch.%s.remote", branch);
        git_config_delete_entry(cfg, key);
        snprintf(key, sizeof(key), "branch.%s.merge", branch);
        git_config_delete_entry(cfg, key);
        git_config_free(cfg);
    }
    return 0;
}

/* Transport */

/* Clone with credentials. */
static inline int gitw_clone(const char *url, const char *path,
                             git_credential_acquire_cb cb, void *payload) {
    git_clone_options opts;
    if (git_clone_options_init(&opts, GIT_CLONE_OPTIONS_VERSION) < 0) return -1;
    opts.fetch_opts.callbacks.credentials = cb;
    opts.fetch_opts.callbacks.payload = payload;
    git_repository *repo = NULL;
    int rc = git_clone(&repo, url, path, &opts);
    if (repo) git_repository_free(repo);
    return rc;
}

/* Fetch from a named remote. */
static inline int gitw_fetch(git_repository *repo, const char *remote_name,
                             git_credential_acquire_cb cb, void *payload) {
    git_remote *remote = NULL;
    git_fetch_options opts;
    int rc = git_remote_lookup(&remote, repo, remote_name);
    if (rc == 0) rc = git_fetch_options_init(&opts, GIT_FETCH_OPTIONS_VERSION);
    if (rc == 0) {
        opts.callbacks.credentials = cb;
        opts.callbacks.payload = payload;
        rc = git_remote_fetch(remote, NULL, &opts, NULL);
    }
    if (remote) git_remote_free(remote);
    return rc;
}

/* Push the current branch to the same-named branch on the remote. */
static inline int gitw_push(git_repository *repo, const char *remote_name,
                            git_credential_acquire_cb cb, void *payload) {
    char branch[GITW_NAME_MAX];
    if (gitw_current_branch(repo, branch, sizeof(branch)) < 0) return -1;
    git_remote *remote = NULL;
    git_push_options opts;
    git_strarray refspecs;
    char spec[GITW_NAME_MAX * 3];
    char *specs[1];
    snprintf(spec, sizeof(spec), "refs/heads/%s:refs/heads/%s", branch, branch);
    specs[0] = spec;
    refspecs.strings = specs;
    refspecs.count = 1;
    int rc = git_remote_lookup(&remote, repo, remote_name);
    if (rc == 0) rc = git_push_options_init(&opts, GIT_PUSH_OPTIONS_VERSION);
    if (rc == 0) {
        opts.callbacks.credentials = cb;
        opts.callbacks.payload = payload;
        rc = git_remote_push(remote, &refspecs, &opts);
    }
    if (remote) git_remote_free(remote);
    return rc;
}

/* Validate credentials and reachability without cloning: an anonymous remote is
 * connected and its refs listed. Needs no local repository. */
static inline int gitw_ls_remote(const char *url,
                                 git_credential_acquire_cb cb, void *payload) {
    git_remote *remote = NULL;
    git_remote_callbacks cbs;
    git_remote_init_callbacks(&cbs, GIT_REMOTE_CALLBACKS_VERSION);
    cbs.credentials = cb;
    cbs.payload = payload;
    int rc = git_remote_create_anonymous(&remote, NULL, url);
    if (rc == 0) rc = git_remote_connect(remote, GIT_DIRECTION_FETCH, &cbs, NULL, NULL);
    if (rc == 0) {
        const git_remote_head **heads = NULL;
        size_t count = 0;
        rc = git_remote_ls(&heads, &count, remote);
    }
    if (remote) {
        git_remote_disconnect(remote);
        git_remote_free(remote);
    }
    return rc;
}

/* Merge */

/* pull(): 0 up to date, 1 merged, 2 conflicts, -2 no upstream, -3 unborn.
 * -2/-3 are expected states, not failures. */
#define GITW_MERGE_UP_TO_DATE  0
#define GITW_MERGE_MERGED      1
#define GITW_MERGE_CONFLICT    2
#define GITW_MERGE_NO_UPSTREAM (-2)
#define GITW_MERGE_UNBORN      (-3)

/* Branch HEAD points at, via its symbolic target, so it works while unborn. */
static inline int gitw_head_branch_ref(git_repository *repo, char *out, size_t out_len) {
    /* Looking up "HEAD" yields a ref whose name is literally "HEAD"; the
     * branch is its symbolic target. */
    git_reference *head = NULL;
    if (git_repository_head(&head, repo) < 0) {
        git_error_clear();
        git_reference *sym = NULL;
        if (git_reference_lookup(&sym, repo, "HEAD") < 0) return -1;
        const char *target = git_reference_symbolic_target(sym);
        /* Detached HEAD has no target. */
        int ok = target && strncmp(target, "refs/heads/", 11) == 0;
        if (ok) snprintf(out, out_len, "%s", target);
        git_reference_free(sym);
        return ok ? 0 : -1;
    }
    const char *name = git_reference_name(head);
    /* A resolved branch is already refs/heads/X. */
    int ok = name && strncmp(name, "refs/heads/", 11) == 0;
    if (ok) snprintf(out, out_len, "%s", name);
    git_reference_free(head);
    return ok ? 0 : -1;
}

/* Unborn first-sync: create the local branch at the remote commit, check it
 * out, and set tracking. Without this a new notebook never gets remote files. */
static inline int gitw_adopt_remote_branch(git_repository *repo,
                                           const char *remote_name,
                                           const char *branch_name) {
    char refname[GITW_NAME_MAX * 3];
    snprintf(refname, sizeof(refname), "refs/remotes/%s/%s", remote_name, branch_name);

    git_reference *remote_ref = NULL;
    if (git_reference_lookup(&remote_ref, repo, refname) < 0) {
        git_error_clear();
        /* Fall back to the remote's default HEAD (e.g. remote on master). */
        snprintf(refname, sizeof(refname), "refs/remotes/%s/HEAD", remote_name);
        if (git_reference_lookup(&remote_ref, repo, refname) < 0) {
            git_error_clear();
            return -1;
        }
    }
    const git_oid *oid = git_reference_target(remote_ref);
    if (!oid) { git_reference_free(remote_ref); return -1; }
    git_oid target = *oid;

    git_commit *commit = NULL;
    if (git_commit_lookup(&commit, repo, &target) < 0) {
        git_reference_free(remote_ref);
        return -1;
    }

    /* force=1 so a stale branch cannot block the first sync. */
    git_reference *branch = NULL;
    int rc = git_branch_create(&branch, repo, branch_name, commit, 1);
    if (branch) git_reference_free(branch);

    if (rc == 0) {
        char localref[GITW_NAME_MAX * 2];
        snprintf(localref, sizeof(localref), "refs/heads/%s", branch_name);
        rc = git_repository_set_head(repo, localref);
    }
    if (rc == 0) {
        /* FORCE: an empty worktree makes SAFE decline to write files. */
        git_checkout_options copts;
        git_checkout_options_init(&copts, GIT_CHECKOUT_OPTIONS_VERSION);
        copts.checkout_strategy = GIT_CHECKOUT_FORCE;
        rc = git_checkout_tree(repo, (const git_object *)commit, &copts);
    }
    if (rc == 0) {
        /* Tracking config, so later syncs use the normal merge path. */
        git_config *cfg = NULL;
        if (git_repository_config(&cfg, repo) == 0) {
            char key[GITW_NAME_MAX * 2], val[GITW_NAME_MAX * 2];
            snprintf(key, sizeof(key), "branch.%s.remote", branch_name);
            git_config_set_string(cfg, key, remote_name);
            snprintf(key, sizeof(key), "branch.%s.merge", branch_name);
            snprintf(val, sizeof(val), "refs/heads/%s", branch_name);
            git_config_set_string(cfg, key, val);
            git_config_free(cfg);
        }
    }

    git_commit_free(commit);
    git_reference_free(remote_ref);
    return rc;
}

static inline int gitw_merge_upstream(git_repository *repo) {
    char branch[GITW_NAME_MAX];
    if (gitw_current_branch(repo, branch, sizeof(branch)) < 0) {
        /* Fresh notebook: adopt the remote branch instead of doing nothing. */
        char headref[GITW_NAME_MAX * 3];
        if (gitw_head_branch_ref(repo, headref, sizeof(headref)) < 0) {
            git_error_clear();
            return GITW_MERGE_UNBORN;
        }
        const char *slash = strrchr(headref, '/');
        const char *unborn = slash ? slash + 1 : headref;
        int created = gitw_adopt_remote_branch(repo, "origin", unborn);
        git_error_clear();
        return created == 0 ? GITW_MERGE_MERGED : GITW_MERGE_UNBORN;
    }

    char refname[GITW_NAME_MAX * 2];
    snprintf(refname, sizeof(refname), "refs/heads/%s", branch);

    git_reference *branchref = NULL;
    if (git_reference_lookup(&branchref, repo, refname) < 0) return GITW_MERGE_UNBORN;
    git_reference *up = NULL;
    int uprc = git_branch_upstream(&up, branchref);
    git_reference_free(branchref);
    if (uprc < 0) { git_error_clear(); return GITW_MERGE_NO_UPSTREAM; }

    const git_oid *up_oid = git_reference_target(up);
    if (!up_oid) { git_reference_free(up); return GITW_MERGE_NO_UPSTREAM; }
    git_oid up_copy = *up_oid;
    git_reference_free(up);

    git_oid head_oid;
    if (git_reference_name_to_id(&head_oid, repo, "HEAD") < 0) return GITW_MERGE_UNBORN;

    git_annotated_commit *theirs = NULL;
    if (git_annotated_commit_lookup(&theirs, repo, &up_copy) < 0) return -1;

    git_merge_analysis_t analysis;
    git_merge_preference_t pref;
    const git_annotated_commit *heads[1];
    heads[0] = theirs;
    if (git_merge_analysis(&analysis, &pref, repo, heads, 1) < 0) {
        git_annotated_commit_free(theirs);
        return -1;
    }
    if (analysis & GIT_MERGE_ANALYSIS_UP_TO_DATE) {
        git_annotated_commit_free(theirs);
        return GITW_MERGE_UP_TO_DATE;
    }
    if (analysis & GIT_MERGE_ANALYSIS_FASTFORWARD) {
        /* Move the ref FIRST, then check out the new HEAD so the working tree
         * matches it. Doing it the other way round checks out the old commit. */
        git_reference *ref = NULL;
        if (git_reference_lookup(&ref, repo, refname) < 0) {
            git_annotated_commit_free(theirs);
            return -1;
        }
        git_reference *newref = NULL;
        int rc = git_reference_set_target(&newref, ref, &up_copy, "fast-forward");
        if (newref) git_reference_free(newref);
        git_reference_free(ref);
        git_annotated_commit_free(theirs);
        if (rc < 0) return -1;
        /* FORCE: after the ref moves, SAFE reports success but leaves stale
         * file contents, because the index already holds the new tree. */
        git_checkout_options copts;
        git_checkout_options_init(&copts, GIT_CHECKOUT_OPTIONS_VERSION);
        copts.checkout_strategy = GIT_CHECKOUT_FORCE;
        if (git_checkout_head(repo, &copts) < 0) return -1;
        return GITW_MERGE_MERGED;
    }

    int rc = git_merge(repo, heads, 1, NULL, NULL);
    git_annotated_commit_free(theirs);
    if (rc < 0) return -1;
    if (gitw_index_conflict_count(repo) > 0) return GITW_MERGE_CONFLICT;
    if (gitw_commit_merge(repo, "Merge remote-tracking branch") < 0) return -1;
    git_repository_state_cleanup(repo);
    return GITW_MERGE_MERGED;
}
