import { useEffect, useMemo, useState } from "react";
import "./App.css";

const TENANT_ID = "TNT001";
const ROUTE_API = "/api/v1/admin/routes";

const emptyForm = {
  id: "demo-user-route",
  routeName: "Demo User Route",
  publicPath: "/users/**",
  targetUri: "http://httpbin.org",
  stripPrefix: 1,
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
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState({ type: "", text: "" });
  const [form, setForm] = useState(emptyForm);

  const stats = useMemo(() => {
    const total = routes.length;
    const enabled = routes.filter((route) => route.enabled).length;
    const disabled = total - enabled;

    return [
      { label: "Total routes", value: total, tone: "blue" },
      { label: "Enabled", value: enabled, tone: "green" },
      { label: "Disabled", value: disabled, tone: "amber" },
    ];
  }, [routes]);

  const fetchRoutes = async () => {
    setLoading(true);
    try {
      const response = await fetch(ROUTE_API, {
        headers: {
          "X-Tenant-ID": TENANT_ID,
        },
      });

      if (!response.ok) {
        throw new Error("Failed to fetch routes");
      }

      const data = await response.json();
      setRoutes(data || []);
      setNotice({ type: "success", text: "Routes refreshed successfully." });
    } catch (error) {
      setNotice({ type: "error", text: "Unable to load routes." });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRoutes();
  }, []);

  const handleInputChange = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((current) => ({
      ...current,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleCreateRoute = async (event) => {
    event.preventDefault();

    setSaving(true);
    setNotice({ type: "", text: "" });

    const payload = {
      ...form,
      stripPrefix: Number(form.stripPrefix),
      enabled: Boolean(form.enabled),
    };

    try {
      const response = await fetch(ROUTE_API, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Tenant-ID": TENANT_ID,
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Route creation failed");
      }

      await fetchRoutes();
      setForm(emptyForm);
      setNotice({ type: "success", text: "Route added successfully." });
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
      const response = await fetch(`${ROUTE_API}/${routeId}`, {
        method: "DELETE",
      });

      if (!response.ok) {
        throw new Error("Delete failed");
      }

      setRoutes((current) => current.filter((route) => route.id !== routeId));
      setNotice({ type: "success", text: "Route deleted." });
    } catch (error) {
      setNotice({ type: "error", text: "Unable to delete route." });
    }
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="label">Gateway Studio</p>
          <h1>API Routing Console</h1>
        </div>

        <button className="btn btn-secondary" onClick={fetchRoutes} disabled={loading}>
          {loading ? "Refreshing..." : "Refresh"}
        </button>
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
              No routes configured yet. Add your first route to begin routing traffic.
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