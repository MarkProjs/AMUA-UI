import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/users';

export async function fetchCompanies() {
    const res = await axios.get(`${API_BASE_URL}/companies`);
    return res.data;
}

export async function fetchAllBUs() {
    const res = await axios.get(`${API_BASE_URL}/bu/all`);
    return res.data;
}

export async function fetchClasses(company) {
    const res = await axios.get(`${API_BASE_URL}/classes`, {
        params: {company},
    });
    return res.data;
}

export async function fetchBusinessUnit(company, clazz) {
    const res = await axios.get(`${API_BASE_URL}/bu/class`, {
        params: {company, clazz},
    });
    return res.data.length > 0 ? res.data[0].GroupNTDesc : null;
}

export async function searchWithParams(company, clazz, businessUnit) {
    const res = await axios.get(`${API_BASE_URL}/search/param`, {
        params: {company, clazz, businessUnit},
    });
    return res.data;
}

export async function searchAll() {
    const res = await axios.get(`${API_BASE_URL}/search/all`);
    return res.data;
}

export async function searchByCompany(company) {
    const res = await axios.get(`${API_BASE_URL}/search/company`, {
        params: {company},
    });
    return res.data;
}

export async function updateNTAccount(company, clazz, businessUnit, ntAccount) {
    const res = await axios.put(`${API_BASE_URL}/update-nt`, {
        company,
        clazz,
        businessUnit,
        ntAccount
    });
    return res.data;
}

export async function getNTAccountsByCompany(company) {
    const res = await axios.get(`${API_BASE_URL}/nt-accounts`, {
        params: {company}
    });
    return res.data;
}

// export async function addClassToAll(clazz, businessUnit) {
//     const res = await axios.post(`${API_BASE_URL}/add-class/all`, {
//         businessUnit,
//         clazz
//     });

//     return res.data;
// }

export async function addClassForCompany(company, clazz, businessUnit, ntAccount) {
    try {
        const res = await axios.post(`${API_BASE_URL}/add-class/single`, {
            company,
            clazz,
            businessUnit,
            ntAccount
        });
        return { success: true, data: res.data };
    } catch (err) {
        if (err.response && err.response.status === 409) {
            return { success: false, message: err.response.data.message };
        }
        return { success: false, message: 'Unexpected error occurred while adding class.' };
    }
}


export async function deleteClassFromBu(clazz, company) {
    const res = await axios.delete(`${API_BASE_URL}/delete/bu`, {
        params: {clazz, company}
    });
    return res.data
}

export async function deleteRowFromTable(clazz,company) {
    const res = await axios.delete(`${API_BASE_URL}/delete/row`, {
        params: {clazz, company}
    });
    return res.data;
}