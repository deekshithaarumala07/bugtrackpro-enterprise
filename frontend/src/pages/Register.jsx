import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, Bug } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (event) => {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register({ ...form, name: form.name.trim(), email: form.email.trim() });
      navigate('/');
    } catch (e) {
      setError(e.response?.data?.error || 'Registration failed. Check the details and try again.');
    } finally {
      setLoading(false);
    }
  };

  return <main className="authPage">
    <section className="authVisual">
      <div className="visualBrand"><div className="brandIcon"><Bug size={25}/></div><span>BugTrackPro</span></div>
      <div className="visualContent">
        <span className="eyebrow">YOUR TEAM. ONE WORKSPACE.</span>
        <h1>Find bugs.<br/>Fix issues.<br/><span>Ship better software.</span></h1>
        <p>Create an account to report issues, collaborate with your team, and follow work through resolution.</p>
        <div className="visualPoints"><div><span>✓</span> Clear issue tracking</div><div><span>✓</span> Team collaboration</div><div><span>✓</span> Project visibility</div></div>
      </div>
      <p className="visualFooter">A better workflow starts here.</p>
    </section>
    <section className="authFormSide">
      <div className="authCard">
        <div className="mobileBrand"><div className="brandIcon"><Bug size={23}/></div><span>BugTrackPro</span></div>
        <div className="formHeading"><span className="eyebrow">GET STARTED</span><h2>Create your account</h2><p>New registrations receive the Tester role.</p></div>
        <form onSubmit={submit}>
          <label htmlFor="register-name">Full name</label>
          <input id="register-name" autoComplete="name" maxLength="120" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required/>
          <label htmlFor="register-email">Email address</label>
          <input id="register-email" type="email" autoComplete="email" maxLength="254" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} required/>
          <label htmlFor="register-password">Password</label>
          <input id="register-password" type="password" autoComplete="new-password" minLength="12" maxLength="128" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} required/>
          <small>Use at least 12 characters, including an uppercase letter and a number.</small>
          {error && <div className="authError" role="alert">{error}</div>}
          <button type="submit" className="authSubmit" disabled={loading}>{loading ? 'Creating account...' : 'Create tester account'} {!loading && <ArrowRight size={18}/>}</button>
        </form>
        <p className="authSwitch">Already registered? <Link to="/login">Sign in</Link></p>
        <p className="authLegal">Sign in only with an account you are authorized to use.</p>
      </div>
    </section>
  </main>;
}
