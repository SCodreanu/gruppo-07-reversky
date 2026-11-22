"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import type { FormEvent } from "react";
import styles from "./auth-form.module.css";

export type AuthMode = "login" | "register" | "forgot" | "reset";

type FieldViolation = { field: string; message: string };
type ApiError = { code?: string; message?: string; errors?: FieldViolation[] };
type AuthFormProps = { mode: AuthMode; token?: string; nextUrl?: string; registered?: boolean };

const copy = {
  login: {
    eyebrow: "Welcome back",
    title: "Sign in",
    description: "Continue your journey beyond the veil.",
    button: "Sign in",
  },
  register: {
    eyebrow: "Your first step",
    title: "Create an account",
    description: "Choose a name. The threshold is waiting.",
    button: "Create my account",
  },
  forgot: {
    eyebrow: "Get back in",
    title: "Forgot password",
    description: "We’ll email you a link to set a new one.",
    button: "Send reset link",
  },
  reset: {
    eyebrow: "Secure your account",
    title: "New password",
    description: "Choose a password you haven’t used before.",
    button: "Update password",
  },
} satisfies Record<AuthMode, { eyebrow: string; title: string; description: string; button: string }>;

function localizeError(error: ApiError, mode: AuthMode): string {
  switch (error.code) {
    case "AUTH_INVALID_CREDENTIALS":
      return "That email and password don’t match. Please try again.";
    case "AUTH_ACCOUNT_DISABLED":
      return "This account is temporarily disabled.";
    case "AUTH_USERNAME_TAKEN":
      return "That username is already taken.";
    case "AUTH_EMAIL_TAKEN":
      return "An account with this email already exists.";
    case "AUTH_RESET_TOKEN_INVALID":
      return "This link is invalid or has expired. Request a new one.";
    case "VALIDATION_FAILED":
      return "Check the highlighted fields and try again.";
    case "API_UNAVAILABLE":
      return "The service is unavailable. Please try again shortly.";
    default:
      return mode === "login" ? "Sign in failed. Please try again." : "We couldn’t complete your request. Please try again.";
  }
}

function fieldErrors(error: ApiError): Record<string, string> {
  return (error.errors ?? []).reduce<Record<string, string>>((result, item) => {
    if (item.field && item.message) {
      const field = item.field === "newPassword" ? "password" : item.field;
      result[field] = item.message;
    }
    return result;
  }, {});
}

export function AuthForm({ mode, token = "", nextUrl = "/home", registered = false }: AuthFormProps) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [formError, setFormError] = useState("");
  const [errorCode, setErrorCode] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState("");
  const details = copy[mode];

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setFormError("");
    setErrorCode("");
    setErrors({});

    const data = new FormData(event.currentTarget);
    const email = String(data.get("email") ?? "").trim();
    let endpoint = "/api/auth/login";
    let payload: Record<string, unknown> = { email, password: data.get("password") };

    if (mode === "register") {
      endpoint = "/api/auth/register";
      payload = {
        username: String(data.get("username") ?? "").trim(),
        email,
        password: data.get("password"),
        acceptPrivacy: data.get("acceptPrivacy") === "on",
        acceptTerms: data.get("acceptTerms") === "on",
      };
    } else if (mode === "forgot") {
      endpoint = "/api/auth/forgot-password";
      payload = { email };
    } else if (mode === "reset") {
      endpoint = "/api/auth/reset-password";
      payload = { token, newPassword: data.get("password") };
    }

    try {
      const response = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const error = (await response.json().catch(() => ({}))) as ApiError;
        setErrorCode(error.code ?? "");
        setErrors(fieldErrors(error));
        setFormError(localizeError(error, mode));
        return;
      }

      if (mode === "login") {
        const safeNext = nextUrl.startsWith("/") && !nextUrl.startsWith("//") ? nextUrl : "/home";
        router.replace(safeNext);
        return;
      }

      if (mode === "register") {
        const result = (await response.json().catch(() => ({}))) as { sessionStarted?: boolean };
        router.replace(result.sessionStarted ? "/home?welcome=1" : "/login?registered=1");
        return;
      }

      if (mode === "forgot") {
        setSuccess("If an account exists for this address, you’ll receive an email with instructions.");
        return;
      }

      setSuccess("Password updated. You can now sign in with your new password.");
    } catch {
      setErrorCode("API_UNAVAILABLE");
      setFormError("Connection unavailable. Check your network and try again.");
    } finally {
      setPending(false);
    }
  }

  const invalid = (field: string) => Boolean(errors[field]);

  return (
    <div className={styles.formPanel}>
      <p className="eyebrow">{details.eyebrow}</p>
      <h2 className={`display-type ${styles.title}`}>{details.title}</h2>
      <p className={styles.description}>{details.description}</p>
      {mode === "login" && registered && (
        <p className={`notice ${styles.registeredNotice}`} role="status">
          Account created. If you’re not already signed in, use your new credentials to sign in.
        </p>
      )}

      {success ? (
        <div className={styles.successBlock}>
          <p className="notice" role="status">{success}</p>
          <Link className="button" href="/login">Go to sign in</Link>
          {mode === "forgot" && <span className={styles.helper}>Didn’t receive an email? Check your spam folder too.</span>}
        </div>
      ) : mode === "reset" && !token ? (
        <div className={styles.successBlock}>
          <p className="notice notice--error" role="alert">The password reset link is missing or invalid.</p>
          <Link className="button" href="/forgot-password">Request a new link</Link>
        </div>
      ) : (
        <form className={styles.form} onSubmit={submit} noValidate={false}>
          {formError && <p className="notice notice--error" role="alert">{formError}</p>}
          {mode === "reset" && errorCode === "AUTH_RESET_TOKEN_INVALID" && (
            <Link className={`text-link ${styles.newResetLink}`} href="/forgot-password">Request a new link</Link>
          )}

          {mode === "register" && (
            <div className="form-field">
              <label htmlFor="username">Username</label>
              <input
                className="text-input"
                id="username"
                name="username"
                type="text"
                autoComplete="username"
                minLength={3}
                maxLength={30}
                pattern="[A-Za-z0-9_]{3,30}"
                placeholder="e.g. inverse_rain"
                required
                aria-invalid={invalid("username")}
                aria-describedby={invalid("username") ? "username-error" : undefined}
              />
              {invalid("username") && <span id="username-error" className="input-error">{errors.username}</span>}
            </div>
          )}

          {(mode === "login" || mode === "register" || mode === "forgot") && (
            <div className="form-field">
              <label htmlFor="email">Email</label>
              <input
                className="text-input"
                id="email"
                name="email"
                type="email"
                autoComplete="email"
                maxLength={255}
                placeholder="name@example.com"
                required
                aria-invalid={invalid("email")}
                aria-describedby={invalid("email") ? "email-error" : undefined}
              />
              {invalid("email") && <span id="email-error" className="input-error">{errors.email}</span>}
            </div>
          )}

          {(mode === "login" || mode === "register" || mode === "reset") && (
            <div className="form-field">
              <label htmlFor="password">{mode === "reset" ? "New password" : "Password"}</label>
              <div className={styles.passwordField}>
                <input
                  className="text-input"
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  autoComplete={mode === "login" ? "current-password" : mode === "reset" ? "new-password" : "new-password"}
                  minLength={mode === "login" ? undefined : 8}
                  maxLength={72}
                  pattern={mode === "login" ? undefined : "(?=.*[A-Za-z])(?=.*[0-9]).{8,72}"}
                  required
                  onChange={mode === "reset" ? (event) => {
                    const confirmation = document.getElementById("confirmPassword") as HTMLInputElement | null;
                    if (confirmation?.value) {
                      confirmation.setCustomValidity(
                        confirmation.value === event.currentTarget.value ? "" : "Passwords do not match.",
                      );
                    }
                  } : undefined}
                  aria-invalid={invalid("password")}
                  aria-describedby={invalid("password") ? "password-error" : mode === "login" ? undefined : "password-help"}
                />
                <button
                  className={styles.passwordToggle}
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  aria-label={showPassword ? "Hide password" : "Show password"}
                  aria-pressed={showPassword}
                >
                  {showPassword ? "Hide" : "Show"}
                </button>
              </div>
              {invalid("password") ? (
                <span id="password-error" className="input-error">{errors.password}</span>
              ) : mode !== "login" ? (
                <span id="password-help" className={styles.helper}>8 to 72 characters, with at least one letter and one number.</span>
              ) : null}
            </div>
          )}

          {mode === "reset" && (
            <div className="form-field">
              <label htmlFor="confirmPassword">Confirm password</label>
              <div className={styles.passwordField}>
                <input
                  className="text-input"
                  id="confirmPassword"
                  name="confirmPassword"
                  type={showConfirmPassword ? "text" : "password"}
                  autoComplete="new-password"
                  minLength={8}
                  maxLength={72}
                  required
                  onChange={(event) => {
                    const password = (document.getElementById("password") as HTMLInputElement | null)?.value;
                    event.currentTarget.setCustomValidity(
                      !event.currentTarget.value || event.currentTarget.value === password ? "" : "Passwords do not match.",
                    );
                  }}
                />
                <button
                  className={styles.passwordToggle}
                  type="button"
                  onClick={() => setShowConfirmPassword((value) => !value)}
                  aria-label={showConfirmPassword ? "Hide password confirmation" : "Show password confirmation"}
                  aria-pressed={showConfirmPassword}
                >
                  {showConfirmPassword ? "Hide" : "Show"}
                </button>
              </div>
            </div>
          )}

          {mode === "register" && (
            <div className={styles.consents}>
              <label className={styles.consent}>
                <input name="acceptPrivacy" type="checkbox" required />
                <span>I agree to the <a href="#">Privacy Policy</a>.</span>
              </label>
              <label className={styles.consent}>
                <input name="acceptTerms" type="checkbox" required />
                <span>I agree to the <a href="#">Terms of Service</a>.</span>
              </label>
              {invalid("acceptPrivacy") && <span className="input-error">You must accept the Privacy Policy.</span>}
              {invalid("acceptTerms") && <span className="input-error">You must accept the Terms of Service.</span>}
            </div>
          )}

          {mode === "login" && (
            <div className={styles.forgotLink}><Link className="text-link" href="/forgot-password">Forgot password?</Link></div>
          )}

          <button className="button" type="submit" disabled={pending}>
            {pending ? "Please wait…" : details.button}
            {!pending && <span aria-hidden="true">→</span>}
          </button>
        </form>
      )}

      <div className={styles.bottomLink}>
        {mode === "login" ? (
          <span>Don’t have an account? <Link className="text-link" href="/register">Create account</Link></span>
        ) : mode === "register" ? (
          <span>Already have an account? <Link className="text-link" href="/login">Sign in</Link></span>
        ) : mode === "forgot" ? (
          <Link className="text-link" href="/login">Back to sign in</Link>
        ) : (
          <span>Remember your password? <Link className="text-link" href="/login">Sign in</Link></span>
        )}
      </div>
    </div>
  );
}
