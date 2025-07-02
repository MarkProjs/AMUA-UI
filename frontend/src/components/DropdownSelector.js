import React from 'react';
import Select from 'react-select';

function DropDownSelector({ label, value, options, onChange, placeholder, isSearchable = false }) {
    const formattedOptions = options.map(opt=>
        typeof opt === 'string' ? {value: opt, label: opt} : opt
    );
    return(
        <div className='dropdown-selector' style={{minWidth: '170px'}}>
            <label className='form-label fw-bold'>{label}</label>
            <Select
                options = {formattedOptions}
                value={formattedOptions.find(opt => opt.value === value)}
                onChange={(selected) => onChange({ target: { value: selected?.value || '' } })}
                placeholder={placeholder}
                isSearchable={isSearchable}
                classNamePrefix="react-select"
                menuPortalTarget={document.body}
                menuPosition="fixed"
                styles={{
                    menuPortal: (base) => ({ ...base, zIndex: 9999 }),
                }}
            />
        </div>
    );
}

export default DropDownSelector;