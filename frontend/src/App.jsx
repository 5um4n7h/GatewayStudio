import {useEffect, useMemo, useState} from "react";
import {useMsal} from "@azure/msal-react";
import "./App.css";

const ROUTE_API = "/api/v1/admin/routes";
const TENANTS_API = "/api/v1/admin/routes/tenants";
const DEFAULT_TENANT = "TNT001";

const emptyForm = {
    id: "demo-user-route",
    routeName: "Demo User Route",
    publicPath: "/users/**",
    targetUri: "http://httpbin.org",
    stripPrefix: 1,
    rateLimitRequests: 100,
    rateLimitWindowSeconds: 60,
    maxPayloadSizeMb: 10,
    enabled: true,
};

const formatDate = (value) => {
    if (!value) return "—";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat("en-US", {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "numeric",
        minute: "2-digit",
    }).format(date);
};

export default function App() {
    const { instance, accounts, inProgress } = useMsal();
    const isAuthenticated = inProgress === "none" && accounts.length > 0;

    const getAccessToken = async () => {
        if (!isAuthenticated) {
            throw new Error("User not authenticated");
        }
        const response = await instance.acquireTokenSilent({
            account: accounts[0],
            scopes: ["openid", "profile", "User.Read"],
        });
        return response.accessToken;
    };

    const handleLogout = () => {
        instance.setActiveAccount(null);
        sessionStorage.clear();
        localStorage.clear();
        window.location.href = "http://localhost:5173";
    };

    const handleMicrosoftLogin = () => {
        instance.loginRedirect({
            scopes: ["openid", "profile", "User.Read"],
            prompt: "select_account"
        });
    };

    const userName = accounts[0]?.username || "User";
    const [tenantId, setTenantId] = useState(DEFAULT_TENANT);
    const [tenants, setTenants] = useState([]);
    const [tenantsLoading, setTenantsLoading] = useState(true);

    const [routes, setRoutes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [notice, setNotice] = useState({type: "", text: ""});
    const [form, setForm] = useState(emptyForm);

    const stats = useMemo(() => {
        const total = routes.length;
        const enabled = routes.filter((route) => route.enabled).length;
        const disabled = total - enabled;

        return [
            {label: "Total routes", value: total, tone: "blue"},
            {label: "Enabled", value: enabled, tone: "green"},
            {label: "Disabled", value: disabled, tone: "amber"},
        ];
    }, [routes]);

    const fetchTenants = async () => {
        if (!isAuthenticated) return;

        setTenantsLoading(true);
        try {
            const token = await getAccessToken();
            const response = await fetch(TENANTS_API, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error("Failed to fetch tenants");
            }

            const data = await response.json();
            setTenants(data || []);
        } catch (error) {
            console.error("Error fetching tenants:", error);
            setTenants([DEFAULT_TENANT]);
        } finally {
            setTenantsLoading(false);
        }
    };

    const fetchRoutes = async (tid = tenantId) => {
        if (!isAuthenticated) return;

        setLoading(true);
        try {
            const token = await getAccessToken();
            const response = await fetch(ROUTE_API, {
                headers: {
                    "Authorization": `Bearer ${token}`,
                    "X-Tenant-ID": tid,
                },
            });

            if (!response.ok) {
                throw new Error("Failed to fetch routes");
            }

            const data = await response.json();
            setRoutes(data || []);
            setNotice({type: "success", text: `Routes loaded for tenant: ${tid}`});
        } catch (error) {
            setNotice({type: "error", text: "Unable to load routes."});
            setRoutes([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (!isAuthenticated) return;
        fetchTenants();
    }, [isAuthenticated]);

    useEffect(() => {
        if (!isAuthenticated) return;
        fetchRoutes(tenantId);
    }, [tenantId, isAuthenticated]);

    // Handle tenant selection
    const handleTenantChange = (event) => {
        const newTenantId = event.target.value;
        setTenantId(newTenantId);
    };

    const handleInputChange = (event) => {
        const {name, value, type, checked} = event.target;
        setForm((current) => ({
            ...current,
            [name]: type === "checkbox" ? checked : value,
        }));
    };

    const handleCreateRoute = async (event) => {
        event.preventDefault();

        setSaving(true);
        setNotice({type: "", text: ""});

        const payload = {
            ...form,
            stripPrefix: Number(form.stripPrefix),
            rateLimitRequests: Number(form.rateLimitRequests),
            rateLimitWindowSeconds: Number(form.rateLimitWindowSeconds),
            maxPayloadSizeMb: Number(form.maxPayloadSizeMb),
            enabled: Boolean(form.enabled),
        };

        try {
            const token = await getAccessToken();
            const response = await fetch(ROUTE_API, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${token}`,
                    "X-Tenant-ID": tenantId,
                },
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Route creation failed");
            }

            await fetchRoutes();
            setForm(emptyForm);
            setNotice({type: "success", text: "Route added successfully."});
        } catch (error) {
            setNotice({
                type: "error",
                text: error.message || "Failed to add route.",
            });
        } finally {
            setSaving(false);
        }
    };

    const handleDeleteRoute = async (routeId) => {
        try {
            const token = await getAccessToken();
            const response = await fetch(`${ROUTE_API}/${routeId}`, {
                method: "DELETE",
                headers: {
                    "X-Tenant-ID": tenantId,
                    "Authorization": `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error("Delete failed");
            }

            setRoutes((current) => current.filter((route) => route.id !== routeId));
            setNotice({type: "success", text: "Route deleted."});
        } catch (error) {
            setNotice({type: "error", text: "Unable to delete route."});
        }
    };

    const handleToggleRoute = async (routeId, nextEnabled) => {
        try {
            const token = await getAccessToken();
            const response = await fetch(`${ROUTE_API}/${routeId}/enableOrDisableRoute`, {
                method: "POST",
                headers: {
                    "X-Tenant-ID": tenantId,
                    "Authorization": `Bearer ${token}`,
                },
            });

            if (!response.ok) {
                throw new Error("Update failed");
            }

            setRoutes((current) =>
                current.map((route) =>
                    route.id === routeId ? {...route, enabled: nextEnabled} : route
                )
            );

            setNotice({
                type: "success",
                text: nextEnabled ? "Route enabled." : "Route disabled.",
            });
        } catch (error) {
            setNotice({type: "error", text: "Unable to update route."});
        }
    };

    // toggle all routes for current tenant
    const handleToggleAllRoutes = async (enable) => {
        try {
            setLoading(true);
            const token = await getAccessToken();
            const resp = await fetch(`${ROUTE_API}/enableAll?enabled=${enable}`, {
                method: "POST",
                headers: {
                    "X-Tenant-ID": tenantId,
                    "Authorization": `Bearer ${token}`,
                },
            });
            if (!resp.ok) {
                const text = await resp.text();
                throw new Error(text || "Failed to update routes");
            }
            // reload routes for tenant so UI reflects new state
            await fetchRoutes(tenantId);
            setNotice({type: "success", text: enable ? "All routes enabled" : "All routes disabled"});
        } catch (err) {
            console.error(err);
            setNotice({type: "error", text: "Failed to update all routes"});
        } finally {
            setLoading(false);
        }
    };
    if (inProgress !== "none") {
        return (
            <div className="app-shell">
                <div style={{
                    maxWidth: "420px",
                    margin: "120px auto",
                    padding: "32px",
                    borderRadius: "12px",
                    background: "rgba(255, 255, 255, 0.9)",
                    color: "#ffffff",
                    textAlign: "center"
                }}>
                    <h2>Signing in...</h2>
                </div>
            </div>
        );
    }

    if (!isAuthenticated) {
        return (
            <div className="app-shell">
                <div style={{
                    maxWidth: "420px",
                    margin: "120px auto",
                    padding: "32px",
                    borderRadius: "12px",
                    background: "#111827",
                    color: "#fff",
                    textAlign: "center",
                    boxShadow: "0 12px 30px rgba(0,0,0,0.25)"
                }}>
                    <h2 style={{marginBottom: "16px", color: "#cbd5e1"}}>Gateway Studio</h2>
                    <p style={{marginBottom: "24px", color: "#cbd5e1"}}>
                        Sign in with your Microsoft account to access routes
                    </p>
                    <button
                        onClick={handleMicrosoftLogin}
                        style={{
                            background: "#2563eb",
                            color: "white",
                            border: "none",
                            borderRadius: "8px",
                            padding: "12px 20px",
                            fontSize: "16px",
                            cursor: "pointer"
                        }}
                    >
                        Sign in with Microsoft
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="app-shell">
            <header className="topbar">
                <div>
                    <p className="label">Gateway Studio</p>
                    <h1>API Routing Console</h1>
                </div>

                <div className="topbar-controls">
                    <div className="tenant-selector">
                        <label htmlFor="tenant-dropdown">Tenant:</label>
                        <select
                            id="tenant-dropdown"
                            value={tenantId}
                            onChange={handleTenantChange}
                            disabled={tenantsLoading}
                        >
                            {tenants.map((tid) => (
                                <option key={tid} value={tid}>
                                    {tid} {tid === DEFAULT_TENANT ? "(Default)" : ""}
                                </option>
                            ))}
                        </select>
                    </div>

                    <button
                        className="btn btn-secondary"
                        onClick={() => fetchRoutes()}
                        disabled={loading}
                    >
                        {loading ? "Refreshing..." : "Refresh"}
                    </button>

                    <button
                        className="btn btn-danger"
                        onClick={handleLogout}
                        type="button"
                    >
                        Logout
                    </button>
                    <span style={{marginRight: "12px", color: "#cbd5e1"}}>
    {userName}
</span>
                </div>
            </header>

            <section className="stats-grid">
                {stats.map((stat) => (
                    <div key={stat.label} className={`stat-card tone-${stat.tone}`}>
                        <span>{stat.label}</span>
                        <strong>{stat.value}</strong>
                    </div>
                ))}
            </section>

            <div className="content-grid">
                <section className="panel">
                    <div className="panel-header">
                        <h2>Add Route</h2>
                    </div>

                    <form className="route-form" onSubmit={handleCreateRoute}>
                        <div className="field-group">
                            <label>Route ID</label>
                            <input
                                name="id"
                                value={form.id}
                                onChange={handleInputChange}
                                placeholder="users-get"
                                required
                            />
                        </div>

                        <div className="field-group">
                            <label>Route Name</label>
                            <input
                                name="routeName"
                                value={form.routeName}
                                onChange={handleInputChange}
                                placeholder="User Microservice API"
                                required
                            />
                        </div>

                        <div className="field-group">
                            <label>Public Path</label>
                            <input
                                name="publicPath"
                                value={form.publicPath}
                                onChange={handleInputChange}
                                placeholder="/users/**"
                                required
                            />
                        </div>

                        <div className="field-group">
                            <label>Target URI</label>
                            <input
                                name="targetUri"
                                value={form.targetUri}
                                onChange={handleInputChange}
                                placeholder="http://httpbin.org"
                                required
                            />
                        </div>

                        <div className="form-row">
                            <div className="field-group">
                                <label>Strip Prefix</label>
                                <input
                                    type="number"
                                    name="stripPrefix"
                                    value={form.stripPrefix}
                                    onChange={handleInputChange}
                                    min="0"
                                    max="10"
                                />
                            </div>

                            <div className="field-group checkbox-field">
                                <label htmlFor="enabled">Enabled</label>
                                <input
                                    id="enabled"
                                    type="checkbox"
                                    name="enabled"
                                    checked={form.enabled}
                                    onChange={handleInputChange}
                                />
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="field-group">
                                <label>Rate Limit (requests)</label>
                                <input
                                    type="number"
                                    name="rateLimitRequests"
                                    value={form.rateLimitRequests}
                                    onChange={handleInputChange}
                                    min="1"
                                    max="10000"
                                    placeholder="100"
                                />
                            </div>

                            <div className="field-group">
                                <label>Rate Limit Window (seconds)</label>
                                <input
                                    type="number"
                                    name="rateLimitWindowSeconds"
                                    value={form.rateLimitWindowSeconds}
                                    onChange={handleInputChange}
                                    min="1"
                                    max="3600"
                                    placeholder="60"
                                />
                            </div>

                            <div className="field-group">
                                <label>Max Payload (MB)</label>
                                <input
                                    type="number"
                                    name="maxPayloadSizeMb"
                                    value={form.maxPayloadSizeMb}
                                    onChange={handleInputChange}
                                    min="1"
                                    max="500"
                                    placeholder="10"
                                />
                            </div>
                        </div>

                        <button className="btn btn-primary" type="submit" disabled={saving}>
                            {saving ? "Adding..." : "Add Route"}
                        </button>
                    </form>

                    {notice.text && (
                        <div className={`notice ${notice.type}`}>
                            {notice.text}
                        </div>
                    )}
                </section>

                <section className="panel">
                    <div className="panel-header">
                        <h2>Loaded Routes</h2>
                        <span className="meta-badge">{routes.length} total</span>
                    </div>

                    {loading ? (
                        <div className="loading-state">Loading routes...</div>
                    ) : routes.length === 0 ? (
                        <div className="empty-state">
                            No routes configured for {tenantId}. Add your first route to begin routing traffic.
                        </div>
                    ) : (
                        <div className="table-wrap">
                            <table className="route-table">
                                <thead>
                                <tr>
                                    <th>Route</th>
                                    <th>Public Path</th>
                                    <th>Target</th>
                                    <th>Strip</th>
                                    <th>Rate Limit</th>
                                    <th>Payload (MB)</th>
                                    <th>Status</th>
                                    <th>Updated</th>
                                    <th>Action</th>
                                </tr>
                                </thead>
                                <tbody>
                                {routes.map((route) => (
                                    <tr key={route.id}>
                                        <td>
                                            <div className="route-name">{route.routeName || route.id}</div>
                                            <small>{route.id}</small>
                                        </td>
                                        <td>{route.publicPath}</td>
                                        <td>{route.targetUri}</td>
                                        <td>{route.stripPrefix}</td>
                                        <td>
                                            <small>{route.rateLimitRequests}/{route.rateLimitWindowSeconds}s</small>
                                        </td>
                                        <td>{route.maxPayloadSizeMb}</td>
                                        <td>
                            <span className={`status-badge ${route.enabled ? "enabled" : "disabled"}`}>
                              {route.enabled ? "Enabled" : "Disabled"}
                            </span>
                                        </td>
                                        <td>{formatDate(route.updatedAt)}</td>
                                        <td>
                                            <button
                                                className="btn btn-danger"
                                                type="button"
                                                onClick={() => handleDeleteRoute(route.id)}
                                            >
                                                Delete
                                            </button>
                                            <button
                                                className={`btn ${route.enabled ? "btn-disable" : "btn-enable"}`}
                                                type="button"
                                                onClick={() => handleToggleRoute(route.id, !route.enabled)}
                                            >
                                                {route.enabled ? "Disable" : "Enable"}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </section>
            </div>
        </div>
    );
}