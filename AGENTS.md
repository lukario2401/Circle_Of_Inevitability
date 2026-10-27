# Commit attribution

For every commit created with Codex assistance, include this trailer at the end
of the commit message, separated from the body by a blank line:

```text
Co-authored-by: Codex <noreply@openai.com>
```

Keep the user's configured Git author identity.

# Publishing implemented updates

After implementing and validating an update, commit and push it to GitHub. Use the
requested branch; otherwise an appropriate `codex/` branch is acceptable. Do not
leave completed implementation updates only in the local checkout.
