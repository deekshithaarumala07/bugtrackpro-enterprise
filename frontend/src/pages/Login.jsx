import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Bug,
  Eye,
  EyeOff,
  ShieldCheck,
  ArrowRight
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ email: email.trim(), password });
      navigate('/');
    } catch (err) {
      setError(
        err.response?.data?.error ||
        err.response?.data?.message ||
        'Unable to sign in. Check your email and password.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="authPage">
      <section className="authVisual">
        <div className="visualBrand">
          <div className="brandIcon">
            <Bug size={25} />
          </div>
          <span>BugTrackPro</span>
        </div>

        <div className="visualContent">
          <span className="eyebrow">YOUR TEAM. ONE WORKSPACE.</span>
          <h1>
            Find bugs.
            <br />
            Fix issues.
            <br />
            <span>Ship better software.</span>
          </h1>
          <p>
            Keep every issue organized, collaborate with your team,
            and track progress from discovery to resolution.
          </p>

          <div className="visualPoints">
            <div><span>✓</span> Clear issue tracking</div>
            <div><span>✓</span> Team collaboration</div>
            <div><span>✓</span> Project visibility</div>
          </div>
        </div>

        <p className="visualFooter">
          A better workflow starts here.
        </p>
      </section>

      <section className="authFormSide">
        <div className="authCard">
          <div className="mobileBrand">
            <div className="brandIcon"><Bug size={23} /></div>
            <span>BugTrackPro</span>
          </div>

          <div className="formHeading">
            <span className="eyebrow">WELCOME BACK</span>
            <h2>Sign in to your workspace</h2>
            <p>Enter your account details to continue.</p>
          </div>

          <form onSubmit={submit}>
            <label htmlFor="login-email">Email address</label>
            <input
              id="login-email"
              type="email"
              name="email"
              autoComplete="username"
              placeholder="you@company.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />

            <div className="passwordLabel">
              <label htmlFor="login-password">Password</label>
            </div>

            <div className="passwordInput">
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                name="password"
                autoComplete="current-password"
                placeholder="Enter your password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
              <button
                type="button"
                className="passwordToggle"
                onClick={() => setShowPassword((value) => !value)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword
                  ? <EyeOff size={19} />
                  : <Eye size={19} />}
              </button>
            </div>

            {error && (
              <div className="authError" role="alert">
                {error}
              </div>
            )}

            <button
              type="submit"
              className="authSubmit"
              disabled={loading}
            >
              {loading ? 'Signing in...' : 'Sign in to BugTrackPro'}
              {!loading && <ArrowRight size={18} />}
            </button>
          </form>

          <div className="authSecurity">
            <ShieldCheck size={18} />
            <div>
              <strong>Your workspace is protected</strong>
              <span>Secure authentication and role-based access.</span>
            </div>
          </div>

          <p className="authSwitch">
            New to BugTrackPro? <Link to="/register">Create an account</Link>
          </p>

          <p className="authLegal">
            Sign in only with an account you are authorized to use.
          </p>
        </div>
      </section>
    </main>
  );
}