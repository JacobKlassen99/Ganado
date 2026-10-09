/**
 * Registrar Ganado - Web App
 * Conexión directa a Google Apps Script Web App
 */

const API_URL = "https://script.google.com/macros/s/AKfycbzyFpZ4tqUZ34-wNvDT-SlFOn3Gx4n-Jm2I_kQaKGWM7HEfKGejchmDPu2d_Ph1TufmFA/exec";

// Estado global en memoria de la aplicación
let appData = {
  ganado: [],
  pesajes: []
};

let currentFilterSexo = "Todos";
let currentSelectedAnimal = null;

// Inicialización
document.addEventListener("DOMContentLoaded", () => {
  // Establecer fecha de hoy en formularios
  const today = new Date().toISOString().split("T")[0];
  const dateNac = document.getElementById("ganadoFechaNacimiento");
  const datePesaje = document.getElementById("pesajeFecha");
  if (dateNac) dateNac.value = today;
  if (datePesaje) datePesaje.value = today;

  // Inicializar PWA (Service Worker, Instalación y Actualizaciones)
  initPWA();

  // Cargar datos vivos desde Google Sheets
  fetchData();
});

// Control de Vistas
function showView(viewName) {
  document.querySelectorAll(".view").forEach(v => v.classList.remove("active"));
  const target = document.getElementById(`view-${viewName}`);
  if (target) {
    target.classList.add("active");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  if (viewName === "home") {
    renderHome();
  } else if (viewName === "ver-ganado") {
    renderGanadoList();
  } else if (viewName === "registrar-ganado") {
    populateParentDropdowns();
  } else if (viewName === "registrar-pesaje") {
    populatePesajeDropdown();
  }
}

// Mostrar Notificaciones Flotantes
function showToast(message, type = "success") {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  toast.className = `toast ${type} show`;
  setTimeout(() => {
    toast.className = "toast";
  }, 4000);
}

// Formateo de Fechas Limpias
function formatCleanDate(rawDate) {
  if (!rawDate) return "--";
  const str = String(rawDate).trim();
  if (str.includes("T")) {
    return str.split("T")[0];
  }
  return str;
}

// Limpiar números que vienen como "125.0"
function cleanNumber(val) {
  if (val === null || val === undefined) return "";
  let s = String(val).trim();
  if (s.endsWith(".0")) {
    s = s.substring(0, s.length - 2);
  }
  return s;
}

// Calcular Edad
function calculateAge(fechaNacStr) {
  try {
    const cleanStr = formatCleanDate(fechaNacStr);
    const birthDate = new Date(cleanStr);
    if (isNaN(birthDate.getTime())) return cleanStr;

    const today = new Date();
    let years = today.getFullYear() - birthDate.getFullYear();
    let months = today.getMonth() - birthDate.getMonth();
    let days = today.getDate() - birthDate.getDate();

    if (days < 0) {
      months -= 1;
      const prevMonth = new Date(today.getFullYear(), today.getMonth(), 0);
      days += prevMonth.getDate();
    }
    if (months < 0) {
      years -= 1;
      months += 12;
    }

    if (years > 0) {
      return `${years} ${years === 1 ? "año" : "años"}${months > 0 ? `, ${months} m` : ""}`;
    } else if (months > 0) {
      return `${months} ${months === 1 ? "mes" : "meses"}${days > 0 ? `, ${days} d` : ""}`;
    } else {
      return `${Math.max(0, days)} ${days === 1 ? "día" : "días"}`;
    }
  } catch (e) {
    return fechaNacStr;
  }
}

// 1. CARGA DE DATOS DESDE GOOGLE APPS SCRIPT (GET)
async function fetchData(isManualRefresh = false) {
  const banner = document.getElementById("statusBanner");
  const bannerText = document.getElementById("statusBannerText");
  const refreshBtn = document.getElementById("refreshBtn");

  if (refreshBtn) refreshBtn.classList.add("loading");
  banner.className = "status-banner loading";
  bannerText.textContent = "Sincronizando con Google Sheets...";

  try {
    const response = await fetch(API_URL);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const data = await response.json();

    if (data.success) {
      // Procesar filas de GANADO
      const rawGanado = data.ganado || [];
      const parsedGanado = [];
      const startG = (rawGanado.length > 0 && String(rawGanado[0][0]).toUpperCase() === "ID") ? 1 : 0;

      for (let i = startG; i < rawGanado.length; i++) {
        const row = rawGanado[i];
        if (row && row.length >= 2) {
          const num = cleanNumber(row[1]);
          if (num) {
            parsedGanado.push({
              id: row[0] || (i + 1),
              numeroGanado: num,
              sexo: String(row[2] || "Macho").trim(),
              fechaNacimiento: formatCleanDate(row[3]),
              madre: cleanNumber(row[4]) || null,
              padre: cleanNumber(row[5]) || null,
              estado: String(row[6] || "Actual").trim()
            });
          }
        }
      }

      // Procesar filas de PESAJES
      const rawPesajes = data.pesajes || [];
      const parsedPesajes = [];
      const startP = (rawPesajes.length > 0 && String(rawPesajes[0][0]).toUpperCase() === "ID") ? 1 : 0;

      for (let i = startP; i < rawPesajes.length; i++) {
        const row = rawPesajes[i];
        if (row && row.length >= 4) {
          const num = cleanNumber(row[1]);
          if (num) {
            parsedPesajes.push({
              id: row[0] || (i + 1),
              numeroGanado: num,
              fecha: formatCleanDate(row[2]),
              peso: parseFloat(row[3]) || 0,
              observacion: String(row[4] || "").trim()
            });
          }
        }
      }

      appData.ganado = parsedGanado;
      appData.pesajes = parsedPesajes;

      const nowTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      banner.className = "status-banner success";
      bannerText.textContent = `Sincronizado con Google Sheets (${appData.ganado.length} animales)`;
      document.getElementById("statusBannerTime").textContent = nowTime;

      // Actualizar vista actual
      renderHome();
      populateParentDropdowns();
      populatePesajeDropdown();

      const activeView = document.querySelector(".view.active");
      if (activeView && activeView.id === "view-ver-ganado") {
        renderGanadoList();
      } else if (activeView && activeView.id === "view-ficha-animal" && currentSelectedAnimal) {
        openFichaAnimal(currentSelectedAnimal.numeroGanado);
      }

      if (isManualRefresh) {
        showToast("Datos actualizados desde Google Sheets", "success");
      }
    } else {
      throw new Error(data.error || "Error al responder Google Sheets");
    }
  } catch (err) {
    console.error("Error al obtener datos:", err);
    banner.className = "status-banner error";
    bannerText.textContent = `Error de sincronización: ${err.message}`;
    showToast(`Error de conexión: ${err.message}`, "error");
  } finally {
    if (refreshBtn) refreshBtn.classList.remove("loading");
  }
}

// 2. RENDER PANTALLA PRINCIPAL
function renderHome() {
  const totalGanado = appData.ganado.length;
  const machos = appData.ganado.filter(g => g.sexo.toLowerCase() === "macho").length;
  const hembras = appData.ganado.filter(g => g.sexo.toLowerCase() === "hembra").length;

  document.getElementById("metricTotalGanado").textContent = totalGanado;
  document.getElementById("metricMachos").textContent = machos;
  document.getElementById("metricHembras").textContent = hembras;

  // Lista de últimos pesajes ordenados por fecha descendente
  const recentList = document.getElementById("recentPesajesList");
  const sortedPesajes = [...appData.pesajes].sort((a, b) => new Date(b.fecha) - new Date(a.fecha));
  const latest5 = sortedPesajes.slice(0, 5);

  if (latest5.length === 0) {
    recentList.innerHTML = `<div style="text-align: center; color: var(--text-muted); padding: 12px;">No hay pesajes registrados aún.</div>`;
    return;
  }

  recentList.innerHTML = latest5.map(p => `
    <div class="recent-pesaje-item" onclick="openFichaAnimal('${p.numeroGanado}')" style="cursor: pointer;">
      <div>
        <div class="animal">Animal Nº ${p.numeroGanado}</div>
        <div class="date">${p.fecha}${p.observacion ? ` &bull; ${p.observacion}` : ''}</div>
      </div>
      <div class="weight">${p.peso.toFixed(1)} kg</div>
    </div>
  `).join("");
}

// 3. REGISTRAR GANADO (POST)
let selectedSexo = "Macho";
function selectSexo(sexo) {
  selectedSexo = sexo;
  document.getElementById("labelSexoMacho").classList.toggle("selected", sexo === "Macho");
  document.getElementById("labelSexoHembra").classList.toggle("selected", sexo === "Hembra");
}

function populateParentDropdowns() {
  const selectMadre = document.getElementById("ganadoMadre");
  const selectPadre = document.getElementById("ganadoPadre");
  if (!selectMadre || !selectPadre) return;

  const currentM = selectMadre.value;
  const currentP = selectPadre.value;

  const hembras = appData.ganado.filter(g => g.sexo.toLowerCase() === "hembra");
  const machos = appData.ganado.filter(g => g.sexo.toLowerCase() === "macho");

  selectMadre.innerHTML = `<option value="">-- Sin madre asignada --</option>` +
    hembras.map(h => `<option value="${h.numeroGanado}">Nº ${h.numeroGanado} (${h.estado})</option>`).join("");

  selectPadre.innerHTML = `<option value="">-- Sin padre asignado --</option>` +
    machos.map(m => `<option value="${m.numeroGanado}">Nº ${m.numeroGanado} (${m.estado})</option>`).join("");

  if (currentM) selectMadre.value = currentM;
  if (currentP) selectPadre.value = currentP;
}

async function submitGanado(e) {
  e.preventDefault();
  const btn = document.getElementById("btnSubmitGanado");
  const btnText = document.getElementById("btnSubmitGanadoText");

  const numero = document.getElementById("ganadoNumero").value.trim();
  const fechaNac = document.getElementById("ganadoFechaNacimiento").value;
  const madre = document.getElementById("ganadoMadre").value;
  const padre = document.getElementById("ganadoPadre").value;
  const estado = document.getElementById("ganadoEstado").value;

  if (!numero) {
    showToast("Por favor ingrese el número de ganado", "error");
    return;
  }

  // Comprobar si ya existe
  const exists = appData.ganado.some(g => g.numeroGanado.toUpperCase() === numero.toUpperCase());
  if (exists) {
    if (!confirm(`El animal Nº ${numero} ya existe en la lista. ¿Deseas agregarlo de todos modos?`)) {
      return;
    }
  }

  btn.disabled = true;
  btnText.innerHTML = `<span class="loader"></span> Guardando en Google Sheets...`;

  const payload = {
    action: "addGanado",
    numeroGanado: numero,
    sexo: selectedSexo,
    fechaNacimiento: fechaNac,
    madre: madre || "",
    padre: padre || "",
    estado: estado || "Actual"
  };

  try {
    // Enviamos como text/plain para cumplir con Google Apps Script y evitar problemas de CORS
    const response = await fetch(API_URL, {
      method: "POST",
      headers: { "Content-Type": "text/plain;charset=utf-8" },
      body: JSON.stringify(payload)
    });

    const result = await response.json();
    if (result.success || result.mensaje) {
      showToast(`¡Ganado Nº ${numero} guardado correctamente!`, "success");
      document.getElementById("formGanado").reset();
      document.getElementById("ganadoFechaNacimiento").value = new Date().toISOString().split("T")[0];
      selectSexo("Macho");

      // Recargar datos actualizados desde Google Sheets
      await fetchData();
      showView("ver-ganado");
    } else {
      throw new Error(result.error || "No se pudo guardar el registro");
    }
  } catch (err) {
    console.error("Error al guardar ganado:", err);
    showToast(`Error al guardar: ${err.message}`, "error");
  } finally {
    btn.disabled = false;
    btnText.textContent = "Guardar en Google Sheets";
  }
}

// 4. REGISTRAR PESAJE (POST)
function populatePesajeDropdown(preselectedAnimal = null) {
  const select = document.getElementById("pesajeNumeroGanado");
  if (!select) return;

  const currentVal = preselectedAnimal || select.value;

  select.innerHTML = `<option value="">-- Seleccione un animal --</option>` +
    appData.ganado.map(g => `<option value="${g.numeroGanado}">Nº ${g.numeroGanado} (${g.sexo})</option>`).join("");

  if (currentVal) {
    select.value = currentVal;
    onPesajeAnimalChange();
  }
}

function onPesajeAnimalChange() {
  const select = document.getElementById("pesajeNumeroGanado");
  const refBox = document.getElementById("pesajeRefBox");
  const refVal = document.getElementById("pesajeRefVal");
  const refFecha = document.getElementById("pesajeRefFecha");

  const numero = select.value;
  if (!numero) {
    refBox.style.display = "none";
    return;
  }

  const pesajesAnimal = appData.pesajes
    .filter(p => p.numeroGanado.toUpperCase() === numero.toUpperCase())
    .sort((a, b) => new Date(b.fecha) - new Date(a.fecha));

  if (pesajesAnimal.length > 0) {
    const ultimo = pesajesAnimal[0];
    refVal.textContent = `${ultimo.peso.toFixed(1)} kg`;
    refFecha.textContent = ultimo.fecha;
    refBox.style.display = "block";
  } else {
    refVal.textContent = "Sin pesajes previos";
    refFecha.textContent = "--";
    refBox.style.display = "block";
  }
}

function iniciarPesajeParaAnimal(animal) {
  if (!animal) return;
  showView("registrar-pesaje");
  populatePesajeDropdown(animal.numeroGanado);
}

async function submitPesaje(e) {
  e.preventDefault();
  const btn = document.getElementById("btnSubmitPesaje");
  const btnText = document.getElementById("btnSubmitPesajeText");

  const numero = document.getElementById("pesajeNumeroGanado").value;
  const fecha = document.getElementById("pesajeFecha").value;
  const peso = parseFloat(document.getElementById("pesajePeso").value);
  const observacion = document.getElementById("pesajeObservacion").value.trim();

  if (!numero || isNaN(peso) || peso <= 0) {
    showToast("Por favor complete el animal y un peso válido", "error");
    return;
  }

  btn.disabled = true;
  btnText.innerHTML = `<span class="loader"></span> Guardando en Google Sheets...`;

  const payload = {
    action: "addPesaje",
    numeroGanado: numero,
    fecha: fecha,
    peso: peso,
    observacion: observacion
  };

  try {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: { "Content-Type": "text/plain;charset=utf-8" },
      body: JSON.stringify(payload)
    });

    const result = await response.json();
    if (result.success || result.mensaje) {
      showToast(`¡Pesaje de ${peso} kg registrado correctamente!`, "success");
      document.getElementById("pesajePeso").value = "";
      document.getElementById("pesajeObservacion").value = "";

      // Recargar datos desde Google Sheets
      await fetchData();

      // Abrir la ficha del animal para ver el nuevo pesaje reflejado
      openFichaAnimal(numero);
    } else {
      throw new Error(result.error || "No se pudo guardar el pesaje");
    }
  } catch (err) {
    console.error("Error al guardar pesaje:", err);
    showToast(`Error al guardar pesaje: ${err.message}`, "error");
  } finally {
    btn.disabled = false;
    btnText.textContent = "Guardar Pesaje en Google Sheets";
  }
}

// 5. VER GANADO (LISTA Y FILTROS)
function setFilterSexo(sexo, elem) {
  currentFilterSexo = sexo;
  document.querySelectorAll(".chip").forEach(c => c.classList.remove("active"));
  elem.classList.add("active");
  renderGanadoList();
}

function filterGanadoList() {
  renderGanadoList();
}

function renderGanadoList() {
  const container = document.getElementById("ganadoCardsContainer");
  const query = (document.getElementById("searchInput").value || "").trim().toLowerCase();

  let filtered = appData.ganado.filter(g => {
    const matchQuery = !query || g.numeroGanado.toLowerCase().includes(query);
    const matchSexo = (currentFilterSexo === "Todos") || (g.sexo.toLowerCase() === currentFilterSexo.toLowerCase());
    return matchQuery && matchSexo;
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="card" style="text-align: center; color: var(--text-muted); padding: 24px;">
        No se encontraron animales con el filtro seleccionado.
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.map(g => {
    // Último peso
    const pList = appData.pesajes
      .filter(p => p.numeroGanado.toUpperCase() === g.numeroGanado.toUpperCase())
      .sort((a, b) => new Date(b.fecha) - new Date(a.fecha));
    
    const ultimoPeso = pList.length > 0 ? `${pList[0].peso.toFixed(1)} kg` : "Sin pesaje";
    const badgeClass = g.sexo.toLowerCase() === "hembra" ? "badge-hembra" : "badge-macho";
    const edad = calculateAge(g.fechaNacimiento);

    return `
      <div class="ganado-card" onclick="openFichaAnimal('${g.numeroGanado}')">
        <div class="ganado-card-header">
          <div class="ganado-tag">Nº ${g.numeroGanado}</div>
          <span class="badge ${badgeClass}">${g.sexo}</span>
        </div>
        <div class="ganado-card-details">
          <div class="detail-item"><strong>Edad:</strong> ${edad}</div>
          <div class="detail-item"><strong>Último Peso:</strong> ${ultimoPeso}</div>
          <div class="detail-item"><strong>Estado:</strong> ${g.estado}</div>
          <div class="detail-item"><strong>Pesajes:</strong> ${pList.length}</div>
        </div>
      </div>
    `;
  }).join("");
}

// 6. FICHA DEL ANIMAL, GENEALOGÍA E HISTORIAL
function openFichaAnimal(numero) {
  const animal = appData.ganado.find(g => g.numeroGanado.toUpperCase() === String(numero).toUpperCase());
  if (!animal) {
    showToast(`Animal Nº ${numero} no encontrado`, "error");
    return;
  }

  currentSelectedAnimal = animal;
  showView("ficha-animal");

  document.getElementById("fichaNumero").textContent = `Nº ${animal.numeroGanado}`;
  const badgeSexo = document.getElementById("fichaBadgeSexo");
  badgeSexo.textContent = animal.sexo;
  badgeSexo.className = `badge ${animal.sexo.toLowerCase() === "hembra" ? "badge-hembra" : "badge-macho"}`;

  document.getElementById("fichaFechaNac").textContent = animal.fechaNacimiento || "--";
  document.getElementById("fichaEdad").textContent = calculateAge(animal.fechaNacimiento);
  document.getElementById("fichaEstado").textContent = animal.estado || "Actual";

  // Pesajes del animal ordenados cronológicamente
  const pesajes = appData.pesajes
    .filter(p => p.numeroGanado.toUpperCase() === animal.numeroGanado.toUpperCase())
    .sort((a, b) => new Date(a.fecha) - new Date(b.fecha));

  document.getElementById("fichaTotalPesajes").textContent = pesajes.length;

  // Métricas de Peso
  if (pesajes.length > 0) {
    const primerPeso = pesajes[0].peso;
    const ultimoPeso = pesajes[pesajes.length - 1].peso;
    const gananciaTotal = ultimoPeso - primerPeso;

    document.getElementById("fichaPesoActual").textContent = `${ultimoPeso.toFixed(1)} kg`;
    document.getElementById("fichaGananciaTotal").textContent = `${gananciaTotal >= 0 ? '+' : ''}${gananciaTotal.toFixed(1)} kg`;
  } else {
    document.getElementById("fichaPesoActual").textContent = "-- kg";
    document.getElementById("fichaGananciaTotal").textContent = "-- kg";
  }

  // Genealogía: Madre y Padre con enlace
  const madreElem = document.getElementById("fichaMadre");
  if (animal.madre) {
    const existeM = appData.ganado.some(g => g.numeroGanado.toUpperCase() === animal.madre.toUpperCase());
    if (existeM) {
      madreElem.textContent = `Nº ${animal.madre}`;
      madreElem.className = "number";
      madreElem.onclick = () => openFichaAnimal(animal.madre);
    } else {
      madreElem.textContent = `Nº ${animal.madre}`;
      madreElem.className = "number no-link";
      madreElem.onclick = null;
    }
  } else {
    madreElem.textContent = "No registrada";
    madreElem.className = "number no-link";
    madreElem.onclick = null;
  }

  const padreElem = document.getElementById("fichaPadre");
  if (animal.padre) {
    const existeP = appData.ganado.some(g => g.numeroGanado.toUpperCase() === animal.padre.toUpperCase());
    if (existeP) {
      padreElem.textContent = `Nº ${animal.padre}`;
      padreElem.className = "number";
      padreElem.onclick = () => openFichaAnimal(animal.padre);
    } else {
      padreElem.textContent = `Nº ${animal.padre}`;
      padreElem.className = "number no-link";
      padreElem.onclick = null;
    }
  } else {
    padreElem.textContent = "No registrado";
    padreElem.className = "number no-link";
    padreElem.onclick = null;
  }

  // Hijos registrados
  const hijos = appData.ganado.filter(g =>
    (g.madre && g.madre.toUpperCase() === animal.numeroGanado.toUpperCase()) ||
    (g.padre && g.padre.toUpperCase() === animal.numeroGanado.toUpperCase())
  );

  document.getElementById("fichaCountHijos").textContent = hijos.length;
  const hijosContainer = document.getElementById("fichaHijosList");

  if (hijos.length === 0) {
    hijosContainer.innerHTML = `<span style="color: var(--text-muted); font-size: 0.88rem;">No tiene hijos registrados.</span>`;
  } else {
    hijosContainer.innerHTML = hijos.map(h => `
      <div class="chip" onclick="openFichaAnimal('${h.numeroGanado}')" style="cursor: pointer; background: var(--primary-bg); color: var(--primary-dark); font-weight: 700;">
        Nº ${h.numeroGanado} (${h.sexo})
      </div>
    `).join("");
  }

  // Tabla Historial de Pesajes con peso anterior y aumento
  const tableBody = document.getElementById("fichaPesajesTableBody");
  if (pesajes.length === 0) {
    tableBody.innerHTML = `
      <tr>
        <td colspan="4" style="text-align: center; color: var(--text-muted); padding: 16px;">
          No hay registros de pesaje para este animal.
        </td>
      </tr>
    `;
    return;
  }

  // Invertir para mostrar primero el más reciente en la tabla
  const pesajesDesc = [...pesajes].reverse();
  tableBody.innerHTML = pesajesDesc.map((p, idx) => {
    // El anterior cronológico es el siguiente en el arreglo invertido
    const prev = pesajesDesc[idx + 1];
    let diffText = "--";
    let diffClass = "";

    if (prev) {
      const diff = p.peso - prev.peso;
      if (diff > 0) {
        diffText = `+${diff.toFixed(1)} kg`;
        diffClass = "gain-positive";
      } else if (diff < 0) {
        diffText = `${diff.toFixed(1)} kg`;
        diffClass = "gain-negative";
      } else {
        diffText = `0.0 kg`;
      }
    }

    return `
      <tr>
        <td><strong>${p.fecha}</strong></td>
        <td><strong>${p.peso.toFixed(1)} kg</strong></td>
        <td class="${diffClass}">${diffText}</td>
        <td>${p.observacion || '-'}</td>
      </tr>
    `;
  }).join("");
}

// ==========================================
// PWA: INSTALACIÓN Y ACTUALIZACIONES EN VIVO
// ==========================================

let deferredPrompt = null;
let waitingServiceWorker = null;
let swRegistration = null;

// 1. Inicializar PWA
function initPWA() {
  // Comprobar si ya está instalada o ejecutándose en modo standalone
  const isStandalone = window.matchMedia('(display-mode: standalone)').matches ||
                       window.navigator.standalone === true;

  if (isStandalone) {
    hideInstallUI();
  }

  // Capturar evento beforeinstallprompt (Chrome / Android / Edge)
  window.addEventListener('beforeinstallprompt', (e) => {
    // Evitar que el mininfobar automático aparezca
    e.preventDefault();
    deferredPrompt = e;
    showInstallUI();
  });

  // Evento cuando la aplicación fue instalada con éxito
  window.addEventListener('appinstalled', () => {
    deferredPrompt = null;
    hideInstallUI();
    showToast('¡Registrar Ganado se instaló correctamente!', 'success');
  });

  // Registrar Service Worker para soporte offline y actualizaciones automáticas
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js')
      .then((reg) => {
        swRegistration = reg;

        // Comprobar si ya hay un worker esperando activación
        if (reg.waiting) {
          waitingServiceWorker = reg.waiting;
          notifyUpdateAvailable();
        }

        // Detectar si se está instalando un nuevo service worker
        reg.addEventListener('updatefound', () => {
          const newWorker = reg.installing;
          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                // Nuevo contenido disponible
                waitingServiceWorker = newWorker;
                notifyUpdateAvailable();
              }
            });
          }
        });

        // Buscar actualizaciones periódicamente cada 15 minutos en segundo plano
        setInterval(() => {
          try {
            reg.update();
          } catch (e) {
            console.error('Error al verificar actualización:', e);
          }
        }, 15 * 60 * 1000);
      })
      .catch((err) => {
        console.warn('Error al registrar Service Worker:', err);
      });

    // Escuchar cuando el nuevo Service Worker toma el control para recargar
    let refreshing = false;
    navigator.serviceWorker.addEventListener('controllerchange', () => {
      if (!refreshing) {
        refreshing = true;
        window.location.reload();
      }
    });
  }
}

// 2. Control de la interfaz de instalación
function showInstallUI() {
  const headerBtn = document.getElementById('headerInstallBtn');
  const homeBanner = document.getElementById('homeInstallBanner');
  if (headerBtn) headerBtn.style.display = 'flex';
  if (homeBanner) homeBanner.style.display = 'flex';
}

function hideInstallUI() {
  const headerBtn = document.getElementById('headerInstallBtn');
  const homeBanner = document.getElementById('homeInstallBanner');
  if (headerBtn) headerBtn.style.display = 'none';
  if (homeBanner) homeBanner.style.display = 'none';
}

// Ejecutar acción al hacer clic en "Instalar aplicación"
async function triggerInstallPrompt() {
  if (deferredPrompt) {
    try {
      // Mostrar el diálogo nativo de instalación
      deferredPrompt.prompt();
      const choiceResult = await deferredPrompt.userChoice;
      if (choiceResult.outcome === 'accepted') {
        deferredPrompt = null;
        hideInstallUI();
      }
    } catch (err) {
      console.error('Error al invocar instalación:', err);
      openInstallGuide();
    }
  } else {
    // Si el navegador no soporta prompt programático (como iOS Safari o si ya se mostró),
    // mostramos la guía visual con pasos claros paso a paso
    openInstallGuide();
  }
}

// Mostrar y cerrar modal con guía manual
function openInstallGuide() {
  const modal = document.getElementById('installGuideModal');
  if (modal) modal.classList.add('active');
}

function closeInstallGuide(event) {
  if (event && event.target && event.target.id !== 'installGuideModal' && !event.target.classList.contains('modal-close-btn') && event.target.tagName !== 'BUTTON') {
    return;
  }
  const modal = document.getElementById('installGuideModal');
  if (modal) modal.classList.remove('active');
}

// 3. Notificación de Nueva Versión (Actualizaciones PWA)
function notifyUpdateAvailable() {
  const bar = document.getElementById('updateNotificationBar');
  if (bar) {
    bar.style.display = 'block';
  }
}

// Opción: "Descartar"
// Cierra el aviso temporalmente para no molestar la sesión actual,
// pero volverá a aparecer en la siguiente entrada mientras la actualización siga pendiente.
function dismissUpdatePrompt() {
  const bar = document.getElementById('updateNotificationBar');
  if (bar) {
    bar.style.display = 'none';
  }
}

// Verificar si hay datos no guardados en los formularios
function hasUnsavedData() {
  // Comprobar formulario de Ganado
  const numGanado = document.getElementById('ganadoNumero');
  if (numGanado && numGanado.value.trim() !== '') {
    return 'Tienes datos ingresados en el formulario de Registrar Ganado.';
  }

  // Comprobar formulario de Pesaje
  const pesoInput = document.getElementById('pesajePeso');
  if (pesoInput && pesoInput.value.trim() !== '') {
    return 'Tienes datos ingresados en el formulario de Registrar Pesaje.';
  }

  return null;
}

// Opción: "Actualizar ahora"
async function applyAppUpdate() {
  // 1. Proteger información sin guardar
  const unsavedMsg = hasUnsavedData();
  if (unsavedMsg) {
    const confirmUpdate = window.confirm(
      `${unsavedMsg}\n\nSi actualizas ahora, los datos no guardados se perderán.\n¿Deseas continuar y actualizar la aplicación de todos modos?`
    );
    if (!confirmUpdate) {
      return;
    }
  }

  const btn = document.getElementById('btnApplyUpdate');
  if (btn) {
    btn.innerHTML = `<span class="loader" style="width: 14px; height: 14px; vertical-align: middle;"></span> Actualizando...`;
    btn.disabled = true;
  }

  // Si hay un service worker en espera, le enviamos el mensaje SKIP_WAITING
  if (waitingServiceWorker) {
    waitingServiceWorker.postMessage({ type: 'SKIP_WAITING' });
  } else if (swRegistration && swRegistration.waiting) {
    swRegistration.waiting.postMessage({ type: 'SKIP_WAITING' });
  } else {
    // Si por alguna razón no está referenciado directamente, recargamos limpiando caché
    if ('caches' in window) {
      try {
        const keys = await caches.keys();
        await Promise.all(keys.map(k => caches.delete(k)));
      } catch (e) {
        console.warn('Error al limpiar caché:', e);
      }
    }
    window.location.reload();
  }
}

